module com.example.simple_music_player {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.simple_music_player to javafx.fxml;
    exports com.example.simple_music_player;
}