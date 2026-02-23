package model.npc;

import game.entity.Position;
import lombok.Getter;

@Getter
public enum Go {
    DESA_SRIGALA(new Position(1, 480, 360)),
    KOTA_EMAS(new Position(33, 432, 480)),
    KOTA_PELABUHAN(new Position(67, 576, 222)),
    GUA_API(new Position(4, 888, 672)),
    HUTAN_ILUSI(new Position(5, 43 * 24, 36 * 24)),
    LEMBAH_MISTERIUS(new Position(8, 576, 222)),
    DANAU_KENANGAN(new Position(9, 1243, 876)),
    PESISIR(new Position(11, 192, 264)),
    JURANG_BATU(new Position(12, 240, 732)),
    KARANG_TERSEMBUNYI(new Position(13, 150, 979)),
    RAWA(new Position(15, 469, 1093)),
    KUIL_KUNO(new Position(16, 673, 1093)),
    GUA_KELALAWAR(new Position(17, 660, 612)),

    GURUN(new Position(20, 787, 966)),
    JURANG_TENGGELAM(new Position(22, 120, 678)),
    KUBURAN_PASIR(new Position(24, 576, 222)),
    MATA_AIR_HANTU(new Position(26, 576, 222)),
    MAKAM_LT1(new Position(29, 576, 222)),
    MAKAM_LT2(new Position(30, 360, 624)),
    MAKAM_LT3(new Position(31, 360, 624)),
    DARATAN_TINGGI(new Position(37, 150, 674)),
    TEBING_CURAM(new Position(39, 199, 882)),
    DUNIA_ATAS(new Position(41, 187, 462)),
    BAWAH_TANAH(new Position(43, 228, 43)),
    GERBANG_DUNIA_BAWAH(new Position(45, 576, 222)),
    TAMAN(new Position(50, 300, 300)),
    ZONA_PERANG(new Position(104, 300, 300)),
    DESA_BARAT(new Position(105, 192, 120)),
    DESA_UTARA(new Position(106, 8*24, 12*24)),
    DESA_SELATAN(new Position(107, 20*24, 9*24)),
    DESA_TIMUR(new Position(108, 22*24, 8*24));

    private final Position position;

    Go(Position position) {
        this.position = position;
    }

}
