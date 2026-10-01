package com.timetracker; // ajuste para o pacote do seu projeto

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class TrackedAppControllerTest {

    private TrackedAppRepository repo;
    private ForegroundWatcher watcher;
    private TrackedAppController controller;

    @BeforeEach
    void setUp() {
        repo = mock(TrackedAppRepository.class);
        watcher = mock(ForegroundWatcher.class);
        controller = new TrackedAppController(repo, watcher);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static TrackedApp app(String name, String exe, long used, long limit) {
        TrackedApp a = new TrackedApp();
        a.id = 1L;
        a.name = name;
        a.exe = exe;
        a.usedSeconds = used;
        a.limitSeconds = limit;
        a.usageDate = LocalDate.now();
        return a;
    }

    private void assertAddRejeita(TrackedAppController.NewApp body) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> controller.add(body));
        assertEquals(400, e.getStatusCode().value());
        verify(repo, never()).save(any());
    }

    // ---------- POST: validação ----------

    @Test
    void addRejeitaNomeVazio() {
        assertAddRejeita(new TrackedAppController.NewApp("   ", "Overwatch.exe", 7200));
    }

    @Test
    void addRejeitaNomeNulo() {
        assertAddRejeita(new TrackedAppController.NewApp(null, "Overwatch.exe", 7200));
    }

    @Test
    void addRejeitaExecutavelVazio() {
        assertAddRejeita(new TrackedAppController.NewApp("Overwatch", "  ", 7200));
    }

    @Test
    void addRejeitaExecutavelNulo() {
        assertAddRejeita(new TrackedAppController.NewApp("Overwatch", null, 7200));
    }

    @Test
    void addRejeitaLimiteZero() {
        assertAddRejeita(new TrackedAppController.NewApp("Overwatch", "Overwatch.exe", 0));
    }

    @Test
    void addRejeitaLimiteNegativo() {
        assertAddRejeita(new TrackedAppController.NewApp("Overwatch", "Overwatch.exe", -60));
    }

    // ---------- POST: salvando ----------

    @Test
    void addSalvaComTrimEComDataDeHoje() {
        TrackedAppController.AppView v =
                controller.add(new TrackedAppController.NewApp("  Overwatch ", " Overwatch.exe ", 7200));

        ArgumentCaptor<TrackedApp> captor = ArgumentCaptor.forClass(TrackedApp.class);
        verify(repo).save(captor.capture());
        TrackedApp salvo = captor.getValue();
        assertEquals("Overwatch", salvo.name);
        assertEquals("Overwatch.exe", salvo.exe);
        assertEquals(7200L, salvo.limitSeconds);
        assertEquals(0L, salvo.usedSeconds);
        assertEquals(LocalDate.now(), salvo.usageDate);
        assertEquals("Overwatch", v.name());
    }

    // ---------- GET ----------

    @Test
    void listMarcaApenasOAppEmFoco() {
        TrackedApp ow = app("Overwatch", "Overwatch.exe", 0, 7200);
        TrackedApp br = app("Brave", "brave.exe", 0, 14400);
        when(repo.findAll()).thenReturn(List.of(ow, br));
        when(watcher.currentExe()).thenReturn("BRAVE.EXE");   // comparação ignora maiúsculas

        List<TrackedAppController.AppView> views = controller.list();

        assertEquals(2, views.size());
        assertFalse(views.get(0).running());
        assertTrue(views.get(1).running());
    }

    @Test
    void listNaoMarcaNenhumQuandoNaoHaAppEmFoco() {
        when(repo.findAll()).thenReturn(List.of(app("Overwatch", "Overwatch.exe", 0, 7200)));
        when(watcher.currentExe()).thenReturn("");

        assertFalse(controller.list().get(0).running());
    }

    // ---------- PUT /limit ----------

    @Test
    void setLimitAtualizaOLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        when(repo.findById(1L)).thenReturn(Optional.of(a));

        TrackedAppController.AppView v = controller.setLimit(1L, new TrackedAppController.NewLimit(3600));

        assertEquals(3600L, a.limitSeconds);
        assertEquals(3600L, v.limitSeconds());
    }

    @Test
    void setLimitDevolve404QuandoOAppNaoExiste() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.setLimit(99L, new TrackedAppController.NewLimit(3600)));

        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void setLimitRejeitaLimiteZeroOuNegativo() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        when(repo.findById(1L)).thenReturn(Optional.of(a));

        ResponseStatusException zero = assertThrows(ResponseStatusException.class,
                () -> controller.setLimit(1L, new TrackedAppController.NewLimit(0)));
        ResponseStatusException negativo = assertThrows(ResponseStatusException.class,
                () -> controller.setLimit(1L, new TrackedAppController.NewLimit(-60)));

        assertEquals(400, zero.getStatusCode().value());
        assertEquals(400, negativo.getStatusCode().value());
        assertEquals(7200L, a.limitSeconds);   // o limite original não mudou
    }

    // ---------- DELETE ----------

    @Test
    void removeApagaPeloId() {
        controller.remove(1L);

        verify(repo).deleteById(1L);
    }
}