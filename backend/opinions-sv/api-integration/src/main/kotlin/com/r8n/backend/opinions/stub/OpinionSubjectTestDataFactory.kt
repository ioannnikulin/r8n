package com.r8n.backend.opinions.stub

import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import java.util.UUID

object OpinionSubjectTestDataFactory {
    val cafeEinsAReferent =
        ReferentDto(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            "Cafe Eins",
            "Berlin, Alexanderplatz 1",
            52.5217457,
            13.4097131,
        )

    val cafeEinsGReferent =
        ReferentDto(
            UUID.fromString("01010101-0101-0101-0101-010101010101"),
            "Cafe Eins",
            "Berlin, Görlitzer str 1",
            52.49921333621459,
            13.432348384513238,
        )

    val cafeEinsA =
        OpinionSubjectDto(
            UUID.fromString("02020202-0202-0202-0202-020202020202"),
            "Cafe Eins",
            cafeEinsAReferent,
            listOf(cafeEinsGReferent),
        )

    val cafeZweiReferent =
        ReferentDto(
            UUID.fromString("03030303-0303-0303-0303-030303030303"),
            "Cafe Zwei",
            "Berlin, Alexanderplatz 2",
            52.5217458,
            13.4097132,
        )

    val cafeZwei =
        OpinionSubjectDto(
            UUID.fromString("04040404-0404-0404-0404-040404040404"),
            "Cafe Zwei",
            cafeZweiReferent,
            emptyList(),
        )

    val cafeDreiReferent =
        ReferentDto(
            UUID.fromString("05050505-0505-0505-0505-050505050505"),
            "Cafe Drei",
            "Berlin, Görlitzer str. 2",
            52.49921333621460,
            13.432348384513240,
        )

    val cafeDrei =
        OpinionSubjectDto(
            UUID.fromString("06060606-0606-0606-0606-060606060606"),
            "Cafe Drei",
            cafeDreiReferent,
            emptyList(),
        )

    val alexanderReferent =
        ReferentDto(
            UUID.fromString("07070707-0707-0707-0707-070707070707"),
            "Alexander",
            "no address, this is a private person",
            null,
            null,
        )

    val alexanderBlog =
        ReferentDto(
            UUID.fromString("08080808-0808-0808-0808-080808080808"),
            "coffee blog",
            "https://coffeeblog.example",
            null,
            null,
        )

    val alexander =
        OpinionSubjectDto(
            UUID.fromString("09090909-0909-0909-0909-090909090909"),
            "coffee expert Alexander",
            alexanderReferent,
            listOf(alexanderBlog),
        )

    val bernardReferent =
        ReferentDto(
            UUID.fromString("10101010-1010-1010-1010-101010101010"),
            "Bernard",
            "no address, this is a private person",
            null,
            null,
        )

    val bernard =
        OpinionSubjectDto(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "coffee expert Bernard",
            bernardReferent,
            emptyList(),
        )

    val cappuccino1AReferent =
        ReferentDto(
            UUID.fromString("12121212-1212-1212-1212-121212121212"),
            "cappuccino @ Cafe Eins A",
            cafeEinsAReferent.address,
            cafeEinsAReferent.latitude,
            cafeEinsAReferent.longitude,
        )

    val cappuccino1GReferent =
        ReferentDto(
            UUID.fromString("13131313-1313-1313-1313-131313131313"),
            "cappuccino @ Cafe Eins G",
            cafeEinsGReferent.address,
            cafeEinsGReferent.latitude,
            cafeEinsGReferent.longitude,
        )

    val cappuccino1A =
        OpinionSubjectDto(
            UUID.fromString("14141414-1414-1414-1414-141414141414"),
            "cappuccino @ Cafe Eins A",
            cappuccino1AReferent,
            listOf(cappuccino1GReferent),
        )

    val cappuccino1G =
        OpinionSubjectDto(
            UUID.fromString("15151515-1515-1515-1515-151515151515"),
            "cappuccino @ Cafe Eins G",
            cappuccino1GReferent,
            listOf(cappuccino1AReferent),
        )

    val cappuccino1AthiccReferent =
        ReferentDto(
            UUID.fromString("16161616-1616-1616-1616-161616161616"),
            "cappuccino extra thicc @ Cafe Eins A",
            cafeEinsAReferent.address,
            cafeEinsAReferent.latitude,
            cafeEinsAReferent.longitude,
        )

    val cappuccino1Athicc =
        OpinionSubjectDto(
            UUID.fromString("17171717-1717-1717-1717-171717171717"),
            "cappuccino extra thicc @ Cafe Eins A",
            cappuccino1AthiccReferent,
            emptyList(),
        )

    val cappuccino2Referent =
        ReferentDto(
            UUID.fromString("18181818-1818-1818-1818-181818181818"),
            "cappuccino @ Cafe Zwei",
            cafeZweiReferent.address,
            cafeZweiReferent.latitude,
            cafeZweiReferent.longitude,
        )

    val cappuccino2 =
        OpinionSubjectDto(
            UUID.fromString("19191919-1919-1919-1919-191919191919"),
            "cappuccino @ Cafe Zwei",
            cappuccino2Referent,
            emptyList(),
        )

    val cappuccino3Referent =
        ReferentDto(
            UUID.fromString("20202020-2020-2020-2020-202020202020"),
            "cappuccino @ Cafe Drei",
            cafeDreiReferent.address,
            cafeDreiReferent.latitude,
            cafeDreiReferent.longitude,
        )

    val cappuccino3 =
        OpinionSubjectDto(
            UUID.fromString("21212121-2121-2121-2121-212121212121"),
            "cappuccino @ Cafe Drei",
            cappuccino3Referent,
            emptyList(),
        )

    val donaldReferent =
        ReferentDto(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "Donald John Trump",
            "no address, this is a private person",
            null,
            null,
        )

    val president =
        OpinionSubjectDto(
            UUID.fromString("23232323-2323-2323-2323-232323232323"),
            "Donald John Trump as 47th president of USA",
            donaldReferent,
            emptyList(),
        )

    val venezuelaInvasion =
        OpinionSubjectDto(
            UUID.fromString("24242424-2424-2424-2424-242424242424"),
            "ordered Nicholas Maduro to be kidnapped",
            null,
            emptyList(),
        )

    val canadaTariffs =
        OpinionSubjectDto(
            UUID.fromString("25252525-2525-2525-2525-252525252525"),
            "imposed trade tariffs on Canada",
            null,
            emptyList(),
        )

    val moreMoney =
        OpinionSubjectDto(
            UUID.fromString("26262626-2626-2626-2626-262626262626"),
            "USA gets more money from Canada",
            null,
            emptyList(),
        )

    val brokenTrust =
        OpinionSubjectDto(
            UUID.fromString("27272727-2727-2727-2727-272727272727"),
            "nobody wants to trade with USA anymore",
            null,
            emptyList(),
        )
}
