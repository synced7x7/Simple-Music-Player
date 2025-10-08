package com.example.simple_music_player.Model;

import com.example.simple_music_player.Utility.CompressionUtility;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javafx.embed.swing.SwingFXUtils;

@Getter
@Setter
@NoArgsConstructor
public class Track {
    private String path;
    private int id;

    // Metadata fields
    private String title;
    private String artist;
    private  String album;
    private  String genre;
    private  String year;
    private  String format;
    private  String bitrate;
    private  String sampleRate;
    private  String channels;
    private  String length;

    // Album art
    private Image cover;
    private double coverWidth;
    private double coverHeight;
    private byte[] artworkData;
    private byte[] compressedArtworkData;


    public Track(String filePath) {
        this.path = filePath;

        String t = null, a = null, al = null, g = null, y = null;
        String fmt = null, br = null, sr = null, ch = null;
        int len = 0;
        Image c = null;
        double covW = 0;
        double covH = 0;
        byte[] imageData = null;
        byte[] compImageData = null;

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
                    imageData = art.getBinaryData();
                    c = new Image(new ByteArrayInputStream(imageData));
                    compImageData = CompressionUtility.resizeAndCompress(c, 300, 300, 0.6f);
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

        //System.out.println("Constructor Called for + " + t);
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
        this.artworkData = imageData;
        this.compressedArtworkData = compImageData;
    }

    public Track(String path,
                 String title,
                 String artist,
                 String album,
                 String genre,
                 String year,
                 String format,
                 String bitrate,
                 String sampleRate,
                 String channels,
                 String length,
                 byte[] artworkData) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.year = year;
        this.format = format;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.length = length;
        this.artworkData = artworkData;

        if (artworkData != null) {
            this.cover = new Image(new ByteArrayInputStream(artworkData));
            this.coverWidth = cover.getWidth();
            this.coverHeight = cover.getHeight();
        } else {
            this.cover = null;
            this.coverWidth = 0;
            this.coverHeight = 0;
        }
    }



}
