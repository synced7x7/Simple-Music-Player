module com.example.simple_music_player {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires javafx.media;
    requires jaudiotagger;
    requires TarsosDSP;
    requires java.desktop;
    requires jlayer;
    requires java.sql;
    requires javafx.swing;
    requires mp3agic;


    opens com.example.simple_music_player to javafx.fxml;
    exports com.example.simple_music_player;
    exports com.example.simple_music_player.Controller;
    opens com.example.simple_music_player.Controller to javafx.fxml;
    exports com.example.simple_music_player.Services;
    opens com.example.simple_music_player.Services to javafx.fxml;
}