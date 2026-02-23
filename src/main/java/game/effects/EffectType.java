package game.effects;

public enum EffectType {
    MEDAL, BUFF, DEBUFF;

    public int resolveMedalEffect(int itemId, int tier) {

        return switch (itemId) {

            case 4588 -> switch (tier) {
                case 3, 4, 5 -> 0;
                case 6, 7, 8 -> 1;
                case 9, 10, 11 -> 2;
                case 12, 13, 14 -> 25;
                case 15 -> 26;
                default -> -1;
            };


            case 4587 -> switch (tier) {
                case 3, 4, 5 -> 3;
                case 6, 7, 8 -> 4;
                case 9, 10, 11 -> 5;
                case 12, 13, 14 -> 29;
                case 15 -> 30;
                default -> -1;
            };

            case 4589 -> switch (tier) {
                case 3, 4, 5 -> 9;
                case 6, 7, 8 -> 10;
                case 9, 10, 11 -> 11;
                case 12, 13, 14 -> 27;
                case 15 -> 28;
                default -> -1;
            };

            case 4590 -> switch (tier) {
                case 3, 4, 5 -> 6;
                case 6, 7, 8 -> 7;
                case 9, 10, 11 -> 8;
                case 12, 13, 14 -> 31;
                case 15 -> 32;
                default -> -1;
            };

            default -> -1;
        };
    }
}
