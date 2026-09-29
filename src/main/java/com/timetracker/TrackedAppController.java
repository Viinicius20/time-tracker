package com.timetracker;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/apps")
@CrossOrigin
public class TrackedAppController {
    record AppView(Long id, String name, String exe, long usedSeconds, long limitSeconds, boolean running) {}
    record NewApp(String name, String exe, long limitSeconds) {}
    record NewLimit(long limitSeconds) {}

    private final TrackedAppRepository repo;
    private final ForegroundWatcher watcher;

    public TrackedAppController(TrackedAppRepository repo, ForegroundWatcher watcher) {
        this.repo = repo;
        this.watcher = watcher;
    }

    private AppView view(TrackedApp a) {
        return new AppView(a.id, a.name, a.exe, a.usedSeconds, a.limitSeconds,
                a.exe.equalsIgnoreCase(watcher.currentExe()));
    }

    @GetMapping
    public List<AppView> list() {
        return repo.findAll().stream().map(this::view).toList();
    }

    @PostMapping
    public AppView add(@RequestBody NewApp body) {
        if (body.name() == null || body.name().isBlank() || body.exe() == null || body.exe().isBlank()
                || body.limitSeconds() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, exe e limitSeconds são obrigatórios");
        }
        TrackedApp a = new TrackedApp();
        a.name = body.name().trim();
        a.exe = body.exe().trim();
        a.limitSeconds = body.limitSeconds();
        a.usageDate = java.time.LocalDate.now();
        return view(repo.save(a));
    }

    @PutMapping("/{id}/limit")
    public AppView setLimit(@PathVariable Long id, @RequestBody NewLimit body) {
        TrackedApp a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.limitSeconds = body.limitSeconds();
        return view(repo.save(a));
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
