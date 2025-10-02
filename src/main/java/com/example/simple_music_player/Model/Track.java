package com.example.simple_music_player.Model;

import javafx.scene.image.Image;
import lombok.Getter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.ByteArrayInputStream;
import java.io.File;

@Getter
public class Track {
    private final String path;

    // Metadata fields
    private final String title;
    private final String artist;
    private final String album;
    private final String genre;
    private final String year;
    private final String format;
    private final String bitrate;
    private final String sampleRate;
    private final String channels;
    private final String length;

    // Album art
    private final Image cover;
    private final double coverWidth;
    private final double coverHeight;

    public Track(String filePath) {
        this.path = filePath;

        String t = null, a = null, al = null, g = null, y = null;
        String fmt = null, br = null, sr = null, ch = null;
        int len = 0;
        Image c = null;
        double covW = 0;
        double covH = 0;

        try {
            File file = new File(filePath);
            AudioFile audioFile = AudioFileIO.read(file);

            // --- TAG INFO ---
            Tag tag = audioFile.getTag();
            if (tag != null) {
                t  = tag.getFirst(FieldKey.TITLE);
                a  = tag.getFirst(FieldKey.ARTIST);
                al = tag.getFirst(FieldKey.ALBUM);
                g  = tag.getFirst(FieldKey.GENRE);
                y  = tag.getFirst(FieldKey.YEAR);

                Artwork art = tag.getFirstArtwork();
                if (art != null) {
                    byte[] imageData = art.getBinaryData();
                    c = new Image(new ByteArrayInputStream(imageData));
                    covW = c.getWidth();
                    covH = c.getHeight();
                }
            }

            // --- AUDIO HEADER INFO ---
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                fmt = header.getFormat();
                br  = header.getBitRate() + "kbps";
                sr  = Integer.parseInt(Integer.toString(Integer.parseInt(header.getSampleRate())/1000)) + "KHz";
                ch  = header.getChannels();
                len = header.getTrackLength();   //seconds
            }

        } catch (Exception e) {
            System.out.println("Error reading metadata for: " + filePath);
            e.printStackTrace();
        }

        String l = Integer.toString(len);

        this.title = (t == null || t.isEmpty()) ? new File(filePath).getName() : t; //if title is empty then read name from file
        this.artist = a;
        this.album = al;
        this.genre = g;
        this.year = y;
        this.format = fmt;
        this.bitrate = br;
        this.sampleRate = sr;
        this.channels = ch;
        this.length = l;
        this.cover = c;
        this.coverWidth = covW;
        this.coverHeight = covH;
    }


}
