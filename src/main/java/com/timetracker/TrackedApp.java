package com.timetracker;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class TrackedApp {
    @Id @GeneratedValue public Long id;
    public String name;          // nome exibido: "Overwatch"
    public String exe;           // executável: "Overwatch.exe"
    public long usedSeconds;     // uso no dia atual
    public long limitSeconds;    // limite diário
    public java.time.LocalDate usageDate;
}
