package com.timetracker; // ajuste para o pacote do seu projeto

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForegroundWatcherTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 1);
    private static final LocalDate AMANHA = HOJE.plusDays(1);
    private static final String TITULO = "Tempo esgotado";

    private TrackedAppRepository repo;
    private Notifier notifier;
    private ForegroundWatcher watcher;

    @BeforeEach
    void setUp() {
        repo = mock(TrackedAppRepository.class);
        notifier = mock(Notifier.class);
        watcher = new ForegroundWatcher(repo, notifier);
    }

    private static TrackedApp app(String name, String exe, long used, long limit, LocalDate usageDate) {
        TrackedApp a = new TrackedApp();
        a.id = 1L;
        a.name = name;
        a.exe = exe;
        a.usedSeconds = used;
        a.limitSeconds = limit;
        a.usageDate = usageDate;
        return a;
    }

    private static TrackedApp app(String name, String exe, long used, long limit) {
        return app(name, exe, used, limit, HOJE);
    }

    private void cadastra(TrackedApp a) {
        when(repo.findAll()).thenReturn(List.of(a));
        when(repo.findFirstByExeIgnoreCase(a.exe)).thenReturn(Optional.of(a));
    }

    // ---------- contagem de tempo ----------

    @Test
    void contaUmSegundoQuandoOAppEstaEmFoco() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        cadastra(a);

        watcher.tick("Overwatch.exe", HOJE);

        assertEquals(1L, a.usedSeconds);
    }

    @Test
    void somaSegundosSeguidos() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        cadastra(a);

        for (int i = 0; i < 5; i++) watcher.tick("Overwatch.exe", HOJE);

        assertEquals(5L, a.usedSeconds);
    }

    @Test
    void naoContaQuandoOutroAppEstaEmFoco() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        cadastra(a);

        watcher.tick("chrome.exe", HOJE);

        assertEquals(0L, a.usedSeconds);
    }

    @Test
    void naoContaQuandoNaoHaJanelaIdentificada() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 7200);
        cadastra(a);

        watcher.tick("", HOJE);

        assertEquals(0L, a.usedSeconds);
        verify(repo, never()).findFirstByExeIgnoreCase(anyString());
    }

    @Test
    void guardaOExecutavelEmFoco() {
        watcher.tick("Overwatch.exe", HOJE);
        assertEquals("Overwatch.exe", watcher.currentExe());

        watcher.tick("", HOJE);
        assertEquals("", watcher.currentExe());
    }

    // ---------- reset diário ----------

    @Test
    void zeraOUsoDeUmDiaAnteriorNaPrimeiraExecucao() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 5000, 7200, HOJE.minusDays(1));
        cadastra(a);

        watcher.tick("", HOJE);

        assertEquals(0L, a.usedSeconds);
        assertEquals(HOJE, a.usageDate);
    }

    @Test
    void mantemOUsoDeHojeQuandoOBackendReiniciaNoMesmoDia() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 100, 7200, HOJE);
        cadastra(a);

        watcher.tick("", HOJE);

        assertEquals(100L, a.usedSeconds);
        assertEquals(HOJE, a.usageDate);
    }

    @Test
    void zeraQuandoViraODiaComOAppRodandoEJaContaOPrimeiroSegundo() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 100, 7200, HOJE);
        cadastra(a);
        watcher.tick("", HOJE);                       // primeira execução, ainda hoje

        watcher.tick("Overwatch.exe", AMANHA);        // virou o dia com o app em foco

        assertEquals(1L, a.usedSeconds);              // zerou e contou o segundo atual
        assertEquals(AMANHA, a.usageDate);
    }

    // ---------- aviso de limite ----------

    @Test
    void avisaAoAtingirOLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 9, 10);
        cadastra(a);

        watcher.tick("Overwatch.exe", HOJE);

        verify(notifier).notify(TITULO, "Você atingiu o limite diário de Overwatch.");
    }

    @Test
    void naoAvisaAntesDoLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 0, 10);
        cadastra(a);

        for (int i = 0; i < 5; i++) watcher.tick("Overwatch.exe", HOJE);

        verify(notifier, never()).notify(anyString(), anyString());
    }

    @Test
    void naoRepeteOAvisoNoSegundoSeguinteAoLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 10, 10);   // já estourou
        cadastra(a);

        watcher.tick("Overwatch.exe", HOJE);                         // 1s depois do limite

        verify(notifier, never()).notify(anyString(), anyString());
    }

    @Test
    void avisaCincoMinutosDepoisDoLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 10 + 299, 10);
        cadastra(a);

        watcher.tick("Overwatch.exe", HOJE);

        verify(notifier).notify(TITULO, "Você já passou 5 min do limite de Overwatch.");
    }

    @Test
    void avisaDezMinutosDepoisDoLimite() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 10 + 599, 10);
        cadastra(a);

        watcher.tick("Overwatch.exe", HOJE);

        verify(notifier).notify(TITULO, "Você já passou 10 min do limite de Overwatch.");
    }

    @Test
    void naoAvisaSeOAppNaoEstaEmFoco() {
        TrackedApp a = app("Overwatch", "Overwatch.exe", 9, 10);
        cadastra(a);

        watcher.tick("chrome.exe", HOJE);

        assertEquals(9L, a.usedSeconds);
        verify(notifier, never()).notify(anyString(), anyString());
    }
}