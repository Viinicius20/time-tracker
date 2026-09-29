package com.timetracker;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ForegroundWatcher {
    private final TrackedAppRepository repo;
    private final Notifier notifier;
    private volatile String currentExe = "";
    private LocalDate day = LocalDate.now();
    private boolean swept = false;

    public ForegroundWatcher(TrackedAppRepository repo, Notifier notifier) {
        this.repo = repo;
        this.notifier = notifier;
    }

    public String currentExe() {
        return currentExe;
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void tick() {
        if (!swept || !day.equals(LocalDate.now())) {   // primeira execução ou virou o dia
            day = LocalDate.now();
            repo.findAll().forEach(a -> {
                if (!day.equals(a.usageDate)) {
                    a.usageDate = day;
                    a.usedSeconds = 0;
                }
            });
            swept = true;
        }
        currentExe = foregroundExe();
        if (!currentExe.isEmpty()) {
            repo.findFirstByExeIgnoreCase(currentExe).ifPresent(a -> {
                a.usedSeconds++;
                long over = a.usedSeconds - a.limitSeconds;
                if (over >= 0 && over % 300 == 0) {   // ao estourar e a cada 5 min depois
                    String msg = over == 0
                            ? "Você atingiu o limite diário de " + a.name + "."
                            : "Você já passou " + (over / 60) + " min do limite de " + a.name + ".";
                    notifier.notify("Tempo esgotado", msg);
                }
            });
        }
    }


    private static String foregroundExe() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "";
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
        HANDLE h = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.getValue());
        if (h == null) return "";
        try {
            char[] buf = new char[1024];
            IntByReference size = new IntByReference(buf.length);
            if (!Kernel32.INSTANCE.QueryFullProcessImageName(h, 0, buf, size)) return "";
            String path = new String(buf, 0, size.getValue());
            return path.substring(path.lastIndexOf('\\') + 1);
        } finally {
            Kernel32.INSTANCE.CloseHandle(h);
        }
    }
}
