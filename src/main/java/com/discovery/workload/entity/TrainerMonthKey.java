package com.discovery.workload.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TrainerMonthKey implements Serializable {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "summary_year", nullable = false)
    private int year;

    @Column(name = "summary_month", nullable = false)
    private int month; // 1-12
}
