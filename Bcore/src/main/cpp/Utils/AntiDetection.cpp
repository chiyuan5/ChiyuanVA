#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <stdarg.h>
#include <pthread.h>
#include <linux/limits.h>
#include "Dobby/dobby.h"
#include "xdl.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ==================== proc 伪造状态 ====================

static char g_guest_pkg[256] = {0};
static char g_host_pkg[256] = {0};
static char g_guest_data_dir[512] = {0};
static char g_host_data_dir[512] = {0};
static pid_t g_self_pid = -1;
static bool g_proc_spoof_enabled = false;

// 宿主特征关键词（GG靠这些判断虚拟环境）
static const char* HOST_SIGNATURES[] = {
    "chiyuanva",
    "chiyuan",
    "libchiyuanva.so",
    "ChiyuanVA",
    "com.chiyuan.va",
    nullptr
};

static bool str_contains_host_sig(const char* str) {
    if (!str) return false;
    for (int i = 0; HOST_SIGNATURES[i] != nullptr; i++) {
        if (strstr(str, HOST_SIGNATURES[i])) return true;
    }
    return false;
}

// ==================== proc 路径判断 ====================

static bool is_proc_cmdline(const char* path) {
    if (!path) return false;
    if (strcmp(path, "/proc/self/cmdline") == 0) return true;
    if (g_self_pid > 0) {
        char buf[64];
        snprintf(buf, sizeof(buf), "/proc/%d/cmdline", g_self_pid);
        if (strcmp(path, buf) == 0) return true;
    }
    return false;
}

static bool is_proc_maps(const char* path) {
    if (!path) return false;
    if (strcmp(path, "/proc/self/maps") == 0) return true;
    if (g_self_pid > 0) {
        char buf[64];
        snprintf(buf, sizeof(buf), "/proc/%d/maps", g_self_pid);
        if (strcmp(path, buf) == 0) return true;
    }
    return false;
}

static bool is_proc_status(const char* path) {
    if (!path) return false;
    if (strcmp(path, "/proc/self/status") == 0) return true;
    if (g_self_pid > 0) {
        char buf[64];
        snprintf(buf, sizeof(buf), "/proc/%d/status", g_self_pid);
        if (strcmp(path, buf) == 0) return true;
    }
    return false;
}

static bool is_proc_mounts(const char* path) {
    if (!path) return false;
    if (strcmp(path, "/proc/self/mountinfo") == 0) return true;
    if (strcmp(path, "/proc/self/mounts") == 0) return true;
    if (g_self_pid > 0) {
        char buf[64];
        snprintf(buf, sizeof(buf), "/proc/%d/mountinfo", g_self_pid);
        if (strcmp(path, buf) == 0) return true;
        snprintf(buf, sizeof(buf), "/proc/%d/mounts", g_self_pid);
        if (strcmp(path, buf) == 0) return true;
    }
    return false;
}

// ==================== 内存fd创建 ====================

static int create_memfd(const char* name) {
    int fd = (int)syscall(__NR_memfd_create, name, 1 /* MFD_CLOEXEC */);
    if (fd >= 0) return fd;
    // 回退临时文件
    char tmpPath[256];
    snprintf(tmpPath, sizeof(tmpPath), "/data/local/tmp/.cva_%s_%d", name, g_self_pid);
    fd = open(tmpPath, O_RDWR | O_CREAT | O_TRUNC, 0600);
    if (fd >= 0) unlink(tmpPath);
    return fd;
}

// ==================== 伪造 proc 文件内容 ====================

// /proc/self/cmdline → 客户App包名
static int create_fake_cmdline() {
    if (g_guest_pkg[0] == '\0') return -1;
    int fd = create_memfd("cl");
    if (fd < 0) return -1;
    write(fd, g_guest_pkg, strlen(g_guest_pkg) + 1);
    lseek(fd, 0, SEEK_SET);
    return fd;
}

// /proc/self/maps → 过滤宿主特征行 + 替换数据路径
static int create_fake_maps() {
    char realPath[64];
    snprintf(realPath, sizeof(realPath), "/proc/%d/maps", g_self_pid);
    // 直接 syscall 避免触发自身 hook
    int realFd = (int)syscall(__NR_openat, AT_FDCWD, realPath, O_RDONLY, 0);
    if (realFd < 0) return -1;

    int fakeFd = create_memfd("mp");
    if (fakeFd < 0) { close(realFd); return -1; }

    FILE* rf = fdopen(realFd, "r");
    if (!rf) { close(realFd); close(fakeFd); return -1; }

    char line[1024];
    while (fgets(line, sizeof(line), rf)) {
        if (str_contains_host_sig(line)) continue;

        if (g_host_data_dir[0] != '\0' && g_guest_data_dir[0] != '\0') {
            char* pos = strstr(line, g_host_data_dir);
            if (pos) {
                char newline[1024];
                size_t prefix_len = pos - line;
                size_t host_len = strlen(g_host_data_dir);
                size_t guest_len = strlen(g_guest_data_dir);
                if (prefix_len + guest_len + strlen(pos + host_len) < sizeof(newline)) {
                    memcpy(newline, line, prefix_len);
                    memcpy(newline + prefix_len, g_guest_data_dir, guest_len);
                    strcpy(newline + prefix_len + guest_len, pos + host_len);
                    write(fakeFd, newline, strlen(newline));
                    continue;
                }
            }
        }
        write(fakeFd, line, strlen(line));
    }
    fclose(rf);
    lseek(fakeFd, 0, SEEK_SET);
    return fakeFd;
}

// /proc/self/status → 修正 Name + TracerPid
static int create_fake_status() {
    char realPath[64];
    snprintf(realPath, sizeof(realPath), "/proc/%d/status", g_self_pid);
    int realFd = (int)syscall(__NR_openat, AT_FDCWD, realPath, O_RDONLY, 0);
    if (realFd < 0) return -1;

    int fakeFd = create_memfd("st");
    if (fakeFd < 0) { close(realFd); return -1; }

    FILE* rf = fdopen(realFd, "r");
    if (!rf) { close(realFd); close(fakeFd); return -1; }

    char line[512];
    while (fgets(line, sizeof(line), rf)) {
        if (strncmp(line, "Name:", 5) == 0 && g_guest_pkg[0] != '\0') {
            const char* shortName = g_guest_pkg;
            const char* dot = strrchr(g_guest_pkg, '.');
            if (dot) shortName = dot + 1;
            char newline[128];
            snprintf(newline, sizeof(newline), "Name:\t%.15s\n", shortName);
            write(fakeFd, newline, strlen(newline));
            continue;
        }
        if (strncmp(line, "TracerPid:", 10) == 0) {
            const char* fakeLine = "TracerPid:\t0\n";
            write(fakeFd, fakeLine, strlen(fakeLine));
            continue;
        }
        write(fakeFd, line, strlen(line));
    }
    fclose(rf);
    lseek(fakeFd, 0, SEEK_SET);
    return fakeFd;
}

// mountinfo/mounts → 过滤宿主特征
static int create_fake_mounts(const char* origPath) {
    int realFd = (int)syscall(__NR_openat, AT_FDCWD, origPath, O_RDONLY, 0);
    if (realFd < 0) return -1;

    int fakeFd = create_memfd("mt");
    if (fakeFd < 0) { close(realFd); return -1; }

    FILE* rf = fdopen(realFd, "r");
    if (!rf) { close(realFd); close(fakeFd); return -1; }

    char line[1024];
    while (fgets(line, sizeof(line), rf)) {
        if (str_contains_host_sig(line)) continue;
        write(fakeFd, line, strlen(line));
    }
    fclose(rf);
    lseek(fakeFd, 0, SEEK_SET);
    return fakeFd;
}

// 统一入口：尝试返回伪造fd，失败返回-1
static int try_fake_proc(const char* path) {
    if (!g_proc_spoof_enabled || !path) return -1;
    if (is_proc_cmdline(path)) return create_fake_cmdline();
    if (is_proc_maps(path))    return create_fake_maps();
    if (is_proc_status(path))  return create_fake_status();
    if (is_proc_mounts(path))  return create_fake_mounts(path);
    return -1;
}

// ==================== 文件隐藏列表（root/虚拟/模拟器/xposed） ====================

static const char* blocked_files[] = {
    "/system/xbin/su", "/system/bin/su", "/sbin/su",
    "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/xbin/daemonsu", "/system/xbin/sugote",
    "/system/bin/sugote-mksh", "/system/xbin/sugote-mksh",
    "/data/local/xbin/su", "/data/local/bin/su", "/data/local/tmp/su",
    "/system/bin/magisk", "/system/xbin/magisk", "/sbin/magisk", "/data/adb/magisk",
    "/data/virtual",
    "/data/data/com.benny.openlauncher", "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual", "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel", "/data/data/com.dual.dualspace",
    "/data/data/com.ludashi.superboost",
    // 注意：不能屏蔽宿主自身路径，否则宿主进程无法读取分身数据
    // "/data/data/com.chiyuan.va" 和 "/chiyuanva" 已移至 proc 伪造层处理
    "/dev/vboxguest", "/dev/vboxuser", "/dev/qemu_pipe", "/dev/goldfish_pipe",
    "/dev/socket/qemud", "/dev/socket/baseband_genyd", "/dev/socket/genyd",
    "/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace",
    "/system/bin/qemu-props", "/system/bin/nox-prop",
    "/sys/module/goldfish_audio", "/sys/module/goldfish_sync",
    "/proc/tty/drivers/goldfish", "/dev/goldfish_events",
    "/system/lib/libdroid4x.so", "/system/bin/windroyed",
    "/system/lib/libnoxspeedup.so", "/system/lib/libmemu.so",
    "/system/lib/libbluelog.so",
    "/system/xposed.prop", "/system/framework/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer",
    "/data/data/org.meowcat.edxposed.manager",
    "/data/data/top.canyie.dreamland.manager",
    nullptr
};

static const char* blocked_packages[] = {
    "com.noshufou.android.su", "com.noshufou.android.su.elite",
    "eu.chainfire.supersu", "com.koushikdutta.superuser",
    "com.thirdparty.superuser", "com.yellowes.su",
    "com.koushikdutta.rommanager", "com.koushikdutta.rommanager.license",
    "com.dimonvideo.luckypatcher", "com.chelpus.lackypatch",
    "com.ramdroid.appquarantine", "com.ramdroid.appquarantinepro",
    "com.devadvance.rootcloak", "com.devadvance.rootcloakplus",
    "de.robv.android.xposed.installer", "com.saurik.substrate",
    "com.zachspong.temprootremovejb",
    "com.amphoras.hidemyroot", "com.amphoras.hidemyrootadfree",
    "com.formyhm.hiderootPremium", "com.formyhm.hideroot",
    "me.phh.superuser", "eu.chainfire.supersu.pro", "com.kingouser.com",
    "com.topjohnwu.magisk", "com.lody.virtual",
    "io.va.exposed", "com.benny.openlauncher",
    nullptr
};

static bool is_blocked_file(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_files[i]; ++i) {
        if (strstr(path, blocked_files[i])) return true;
    }
    return false;
}

static bool is_blocked_package(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_packages[i]; ++i) {
        if (strstr(path, blocked_packages[i])) return true;
    }
    return false;
}

static bool is_safe_path(const char* path) {
    if (!path) return false;
    if (strstr(path, "/proc/net/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    return false;
}

static bool should_block(const char* path) {
    if (!path || is_safe_path(path)) return false;
    return is_blocked_file(path) || is_blocked_package(path);
}

// ==================== 统一 Hook 函数 ====================

static int (*orig_access)(const char *, int) = nullptr;
static int (*orig_stat)(const char *, struct stat *) = nullptr;
static int (*orig_lstat)(const char *, struct stat *) = nullptr;
static FILE* (*orig_fopen)(const char *, const char *) = nullptr;
static int (*orig_open)(const char *, int, ...) = nullptr;
static int (*orig_openat)(int, const char *, int, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *, char *, size_t) = nullptr;
static DIR* (*orig_opendir)(const char *) = nullptr;

static int my_access(const char *pathname, int mode) {
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    return orig_access ? orig_access(pathname, mode) : -1;
}

static int my_stat(const char *pathname, struct stat *buf) {
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    return orig_stat ? orig_stat(pathname, buf) : -1;
}

static int my_lstat(const char *pathname, struct stat *buf) {
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    return orig_lstat ? orig_lstat(pathname, buf) : -1;
}

static FILE* my_fopen(const char *pathname, const char *mode) {
    int fakeFd = try_fake_proc(pathname);
    if (fakeFd >= 0) return fdopen(fakeFd, "r");
    if (should_block(pathname)) { errno = ENOENT; return nullptr; }
    return orig_fopen ? orig_fopen(pathname, mode) : nullptr;
}

static int my_open(const char *pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args; va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    int fakeFd = try_fake_proc(pathname);
    if (fakeFd >= 0) return fakeFd;
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    if (!orig_open) return -1;
    return (flags & O_CREAT) ? orig_open(pathname, flags, mode) : orig_open(pathname, flags);
}

static int my_openat(int dirfd, const char *pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args; va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    int fakeFd = try_fake_proc(pathname);
    if (fakeFd >= 0) return fakeFd;
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    if (!orig_openat) return -1;
    return (flags & O_CREAT) ? orig_openat(dirfd, pathname, flags, mode) : orig_openat(dirfd, pathname, flags);
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (should_block(pathname)) { errno = ENOENT; return -1; }
    ssize_t ret = orig_readlink ? orig_readlink(pathname, buf, bufsiz) : -1;
    // 伪造 /proc/self/exe
    if (g_proc_spoof_enabled && ret > 0 && pathname) {
        bool is_self_exe = (strcmp(pathname, "/proc/self/exe") == 0);
        if (!is_self_exe && g_self_pid > 0) {
            char tmp[64];
            snprintf(tmp, sizeof(tmp), "/proc/%d/exe", g_self_pid);
            is_self_exe = (strcmp(pathname, tmp) == 0);
        }
        if (is_self_exe && g_guest_pkg[0] != '\0') {
            buf[ret < (ssize_t)bufsiz ? ret : bufsiz - 1] = '\0';
            if (str_contains_host_sig(buf)) {
                char fakePath[512];
                snprintf(fakePath, sizeof(fakePath), "/data/app/%s-1/base.apk", g_guest_pkg);
                size_t fakeLen = strlen(fakePath);
                if (fakeLen < bufsiz) {
                    memcpy(buf, fakePath, fakeLen);
                    return (ssize_t)fakeLen;
                }
            }
        }
    }
    return ret;
}

static DIR* my_opendir(const char *name) {
    if (should_block(name)) { errno = ENOENT; return nullptr; }
    return orig_opendir ? orig_opendir(name) : nullptr;
}

// ==================== Hook 安装 ====================

static void install_file_hooks() {
    void* addr;
    addr = DobbySymbolResolver(nullptr, "open");
    if (addr) DobbyHook(addr, (void*)my_open, (void**)&orig_open);
    addr = DobbySymbolResolver(nullptr, "openat");
    if (addr) DobbyHook(addr, (void*)my_openat, (void**)&orig_openat);
    addr = DobbySymbolResolver(nullptr, "fopen");
    if (addr) DobbyHook(addr, (void*)my_fopen, (void**)&orig_fopen);
    addr = DobbySymbolResolver(nullptr, "access");
    if (addr) DobbyHook(addr, (void*)my_access, (void**)&orig_access);
    addr = DobbySymbolResolver(nullptr, "stat");
    if (addr) DobbyHook(addr, (void*)my_stat, (void**)&orig_stat);
    addr = DobbySymbolResolver(nullptr, "lstat");
    if (addr) DobbyHook(addr, (void*)my_lstat, (void**)&orig_lstat);
    addr = DobbySymbolResolver(nullptr, "readlink");
    if (addr) DobbyHook(addr, (void*)my_readlink, (void**)&orig_readlink);
    addr = DobbySymbolResolver(nullptr, "opendir");
    if (addr) DobbyHook(addr, (void*)my_opendir, (void**)&orig_opendir);
    LOGD("All hooks installed via Dobby");
}

// ==================== JNI: 启用 proc 伪造 ====================

extern "C"
void antidetection_enable_proc_spoof(JNIEnv *env, jclass clazz,
    jstring guestPkg, jstring hostPkg)
{
    if (g_proc_spoof_enabled) return;
    g_self_pid = getpid();

    if (guestPkg) {
        const char* pkg = env->GetStringUTFChars(guestPkg, nullptr);
        strncpy(g_guest_pkg, pkg, sizeof(g_guest_pkg) - 1);
        snprintf(g_guest_data_dir, sizeof(g_guest_data_dir), "/data/data/%s", pkg);
        env->ReleaseStringUTFChars(guestPkg, pkg);
    }
    if (hostPkg) {
        const char* pkg = env->GetStringUTFChars(hostPkg, nullptr);
        strncpy(g_host_pkg, pkg, sizeof(g_host_pkg) - 1);
        snprintf(g_host_data_dir, sizeof(g_host_data_dir), "/data/data/%s", pkg);
        env->ReleaseStringUTFChars(hostPkg, pkg);
    }

    g_proc_spoof_enabled = true;
    LOGD("Proc spoof enabled: guest=%s host=%s pid=%d", g_guest_pkg, g_host_pkg, g_self_pid);
}

// ==================== 自动初始化（库加载时） ====================

__attribute__((constructor)) void install_antidetection_hooks() {
    g_self_pid = getpid();
    LOGD("Anti-detection auto hooks disabled, pid=%d", g_self_pid);
}
