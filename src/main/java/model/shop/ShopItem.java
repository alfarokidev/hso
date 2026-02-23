package model.shop;

import lombok.Data;


@Data
public class ShopItem {
    private int itemId;
    private int price;
    private int priceType;
    private int duration;


    public boolean useGem() {return priceType==1;}
}
