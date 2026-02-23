package model.item;


public class FashionHelper {


    public static boolean isMale(int role) {
        return switch (role) {
            case 0, 1 -> true;
            default -> false;
        };
    }
}
