package com.nanobank.ledger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class NanoBankLedgerApplication

fun main(args: Array<String>) {
    runApplication<NanoBankLedgerApplication>(*args)
}
