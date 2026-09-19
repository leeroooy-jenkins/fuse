package fuse;

import jnr.ffi.Pointer;
import jnr.ffi.types.gid_t;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import jnr.ffi.types.uid_t;
import lombok.extern.slf4j.Slf4j;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;
import ru.serce.jnrfuse.struct.Timespec;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SpyFs extends FuseStubFS {
    private final ConcurrentHashMap<String, Long> files = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        String[] extra = Arrays.copyOfRange(args, 1, args.length);
        String[] fuseOpts = new String[3 + extra.length];
        fuseOpts[0] = "-s";
        fuseOpts[1] = "-o";
        fuseOpts[2] = "big_writes";
        System.arraycopy(extra, 0, fuseOpts, 3, extra.length);
        new SpyFs().mount(Paths.get(args[0]), true, false, fuseOpts);
    }

    @Override
    public int getattr(String path, FileStat stat) {
        log.trace("GETATTR path={} pid={}", path, getContext().pid.get());
        if ("/".equals(path)) {
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
            return 0;
        }
        Long size = files.get(path);
        if (size == null) {
            return -ErrorCodes.ENOENT();
        }
        stat.st_mode.set(FileStat.S_IFREG | 0644);
        stat.st_nlink.set(1);
        stat.st_size.set(size);
        stat.st_uid.set(getContext().uid.get());
        stat.st_gid.set(getContext().gid.get());
        return 0;
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo fi) {
        log.info("READDIR path={} pid={}", path, getContext().pid.get());
        filler.apply(buf, ".", null, 0);
        filler.apply(buf, "..", null, 0);
        for (String name : files.keySet()) {
            filler.apply(buf, name.substring(1), null, 0);
        }
        return 0;
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        log.info("CREATE path={} flags={} pid={}", path, octal(fi.flags.get()), getContext().pid.get());
        files.put(path, 0L);
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        log.info("OPEN path={} flags={} pid={}", path, octal(fi.flags.get()), getContext().pid.get());
        if (!files.containsKey(path)) {
            return -ErrorCodes.ENOENT();
        }
        return 0;
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        long pid = getContext().pid.get();
        log.info("WRITE path={} off={} size={} pid={}", path, offset, size, pid);
        byte[] bytes = new byte[(int) size];
        buf.get(0, bytes, 0, (int) size);
        if (log.isDebugEnabled()) {
            log.debug(hexDump(bytes, offset));
        }
        files.put(path, Math.max(files.getOrDefault(path, 0L), offset + size));
        return (int) size;
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        log.warn("READ path={} off={} size={} pid={}", path, offset, size, getContext().pid.get());
        return 0;
    }

    @Override
    public int truncate(String path, @off_t long size) {
        log.info("TRUNCATE path={} size={} pid={}", path, size, getContext().pid.get());
        files.put(path, size);
        return 0;
    }

    @Override
    public int unlink(String path) {
        log.info("UNLINK path={} pid={}", path, getContext().pid.get());
        files.remove(path);
        return 0;
    }

    @Override
    public int rename(String oldpath, String newpath) {
        log.info("RENAME oldpath={} newpath={} pid={}", oldpath, newpath, getContext().pid.get());
        Long size = files.remove(oldpath);
        if (size != null) {
            files.put(newpath, size);
        }
        return 0;
    }

    @Override
    public int flush(String path, FuseFileInfo fi) {
        log.info("FLUSH path={} pid={}", path, getContext().pid.get());
        return 0;
    }
    @Override
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
        log.info("FSYNC path={} pid={}", path, getContext().pid.get());
        return 0;
    }
    @Override
    public int release(String path, FuseFileInfo fi) {
        log.info("RELEASE path={} pid={}", path, getContext().pid.get());
        return 0;
    }
    @Override
    public int chmod(String path, @mode_t long mode) {
        log.info("CHMOD path={} mode={} pid={}", path, octal(mode), getContext().pid.get());
        return 0;
    }
    @Override
    public int chown(String path, @uid_t long uid, @gid_t long gid) {
        log.info("CHOWN path={} uid={} gid={} pid={}", path, uid, gid, getContext().pid.get());
        return 0;
    }
    @Override
    public int utimens(String path, Timespec[] timespec) {
        log.info("UTIMENS path={} pid={}", path, getContext().pid.get());
        return 0;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        log.info("STATFS path={} pid={}", path, getContext().pid.get());
        stbuf.f_bsize.set(4096);
        stbuf.f_frsize.set(4096);
        stbuf.f_blocks.set(1024L * 1024);
        stbuf.f_bfree.set(1024L * 1024);
        stbuf.f_bavail.set(1024L * 1024);
        return 0;
    }

    private static String octal(long value) {
        return "0" + Long.toOctalString(value);
    }

    private static String hexDump(byte[] bytes, long baseOffset) {
        HexFormat hex = HexFormat.of();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 16) {
            int len = Math.min(16, bytes.length - i);
            sb.append(String.format("%08x  ", baseOffset + i));
            for (int j = 0; j < 16; j++) {
                sb.append(j < len ? hex.toHexDigits(bytes[i + j]) : "  ").append(' ');
                if (j == 7) {
                    sb.append(' ');
                }
            }
            sb.append(" |");
            for (int j = 0; j < len; j++) {
                byte b = bytes[i + j];
                sb.append(b >= 0x20 && b < 0x7f ? (char) b : '.');
            }
            sb.append('|');
            if (i + 16 < bytes.length) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
