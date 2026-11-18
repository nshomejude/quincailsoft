/* Recovery-only helper: enable lo inside a newly created user/network namespace.
 * No configurable interface, route, address, external endpoint or shell command.
 * The caller drops all capabilities immediately after this helper returns. */
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char **argv) {
    char current[128];
    if (argc != 2) { fputs("Parent network namespace required\n", stderr); return 1; }
    ssize_t n = readlink("/proc/self/ns/net", current, sizeof(current)-1);
    if (n < 0) { perror("readlink"); return 1; }
    current[n] = '\0';
    if (strcmp(current, argv[1]) == 0) { fputs("Refusing host network namespace\n", stderr); return 1; }
    int fd = socket(AF_INET, SOCK_DGRAM | SOCK_CLOEXEC, 0);
    if (fd < 0) { perror("loopback socket"); return 1; }
    struct ifreq request = {0};
    strcpy(request.ifr_name, "lo");
    if (ioctl(fd, SIOCGIFFLAGS, &request) < 0) { perror("read loopback flags"); close(fd); return 1; }
    request.ifr_flags |= IFF_UP;
    if (ioctl(fd, SIOCSIFFLAGS, &request) < 0) { perror("enable isolated loopback"); close(fd); return 1; }
    close(fd);
    return 0;
}
