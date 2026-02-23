package model.config;

import lombok.Data;

import java.util.List;

@Data
public class AttributeConfig {
    private int role;
    private String name;
    private List<Attribute> strength;
    private List<Attribute> dexterity;
    private List<Attribute> vitality;
    private List<Attribute> intelligence;
}
