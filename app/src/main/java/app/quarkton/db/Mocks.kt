package app.quarkton.db

import app.quarkton.ton.extensions.ZERO_TX

const val MOCK_ADDR_1 = "EQABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrst"
const val MOCK_ADDR_2 = "EQBbBbBbAaAaAaCcCcCcDdDdDdEeEeEeFfFfFfAaAaBbBcc-"
const val MOCK_ADDR_3 = "EQ1234567890abcdefg1234567890abcdegh123745609879"
const val MOCK_ADDR_4 = "EQD126846841B6185168A1685468C615616B64846841D685"
const val MOCK_ADDR_5 = "EQ0001112223334445556667778889991112223334445555"

fun createMockTransaction(now: Long = 0, actOk: Boolean = true, compOk: Boolean = true): TransItem {
    return TransItem(
        id = ZERO_TX,
        acc = MOCK_ADDR_1,
        src = MOCK_ADDR_1,
        dst = MOCK_ADDR_2,
        dst1 = MOCK_ADDR_3,
        dst2 = MOCK_ADDR_4,
        dst3 = MOCK_ADDR_5,
        incmt = "Incoming comment",
        cmt = "Outgoing comment 1",
        cmt1 = "Outgoing comment 2",
        cmt2 = "Outgoing comment 3",
        cmt3 = "Outgoing comment 4",
        inamt = 10_123321123,
        amt = 1_222333444,
        amt1 = 2_234000000,
        amt2 = 3_111222000,
        amt3 = 4_123400000,
        now = now,
        lt = 1,
        totalFee = 123456L,
        storFee = 11111L,
        actFee = 22222L,
        compFee = 33333L,
        fwdFee = 44444L,
        actOk = actOk,
        compOk = compOk,
        prevId = ZERO_TX
    )
}