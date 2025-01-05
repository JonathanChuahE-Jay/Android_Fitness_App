package com.example.quickfeet.models;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class Workout {
    private final String name;
    private final String level;
    private final String instructions;
    private final List<String> images;

    public Workout(String name, String level, String instructions, JSONArray imagesArray) {
        this.name = name;
        this.level = level;
        this.instructions = instructions;
        this.images = new ArrayList<>();
        for (int i = 0; i < imagesArray.length(); i++) {
            this.images.add(imagesArray.optString(i));
        }
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public List<String> getImages() {
        return images;
    }
    public String getInstructions() {
        return instructions;
    }
}
