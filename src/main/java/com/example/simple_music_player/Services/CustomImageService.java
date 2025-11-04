package com.example.simple_music_player.Services;

import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.MiscDAO;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Random;

public class CustomImageService {
    String[] imagePaths = {
            "/x/0.png",
            "/x/1.png",
            "/x/2.png",
            "/x/3.png",
            "/x/4.png",
            "/x/5.png",
            "/x/6.png",
            "/x/7.png",
            "/x/8.png",
            "/x/9.png",
            "/x/10.png",
            "/x/11.png",
            "/x/12.png",
            "/x/13.png",
            "/x/14.png",
            "/x/15.png",
            "/x/16.png",
            "/x/17.png",
            "/x/18.png",
            "/x/19.png"
    };

    private final MiscDAO miscDAO = new MiscDAO(DatabaseManager.getConnection());

    public void setCustomImage(ImageView imageView, int no) {
        String path= imagePaths[no];
        Image image = new Image(Objects.requireNonNull(getClass().getResource(path)).toExternalForm());
        imageView.setImage(image);
    }

    public void toggleCustomImage(ImageView imageView) throws SQLException {
        Random random = new Random();
        int no = random.nextInt(imagePaths.length);
        setCustomImage(imageView, no);
        miscDAO.setCustomImageNo(no);
    }
}
