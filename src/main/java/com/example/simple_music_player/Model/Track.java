package com.example.simple_music_player.Model;

import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Utility.CompressionUtility;
import com.example.simple_music_player.Utility.NotificationUtil;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT;
import org.jaudiotagger.tag.images.Artwork;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

@Getter
@Setter
@NoArgsConstructor
public class Track {
    private String path;
    private int id;

    // Metadata fields
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String year;
    private String format;
    private String bitrate;
    private String sampleRate;
    private String channels;
    private String length;

    // Album art
    private Image cover;
    private double coverWidth;
    private double coverHeight;
    private byte[] artworkData;
    private byte[] compressedArtworkData;
    private String dateAdded;
    private String lyrics;

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
                t = tag.getFirst(FieldKey.TITLE);
                a = tag.getFirst(FieldKey.ARTIST);
                al = tag.getFirst(FieldKey.ALBUM);
                g = tag.getFirst(FieldKey.GENRE);
                y = tag.getFirst(FieldKey.YEAR);

                Artwork art = tag.getFirstArtwork();
                if (art != null) {
                    imageData = art.getBinaryData();
                    c = new Image(new ByteArrayInputStream(imageData));
                    compImageData = CompressionUtility.resizeAndCompress(c, 250, 250, 0.5f);
                    covW = c.getWidth();
                    covH = c.getHeight();
                }
            }

            // --- AUDIO HEADER INFO ---
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                fmt = header.getFormat();
                br = header.getBitRate() + "kbps";
                sr = Integer.parseInt(Integer.toString(Integer.parseInt(header.getSampleRate()) / 1000)) + "KHz";
                ch = header.getChannels();
                len = header.getTrackLength();   //seconds
            }
            this.dateAdded = String.valueOf(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()
            ));

        } catch (Exception e) {
            System.out.println("Error reading metadata for: " + filePath);
            NotificationUtil.alert("Error reading metadata for: " + filePath);
            throw new RuntimeException(e);
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
                 byte[] artworkData,
                 String dateAdded) {
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
        this.dateAdded = dateAdded;

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

    public String getLyrics() throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {

        if (lyrics == null || lyrics.isEmpty()) {
            AudioFile audioFile = AudioFileIO.read(new File(path));
            PlaybackService playbackService = NowPlayingController.getPlaybackService();
            String ext = playbackService.getFileExtension(new File(path));
            if (ext.equals("wav")) return lyrics;
            Tag tag = audioFile.getTag();
            if (tag != null) {
                //Jaudiotagger default lyrics extraction
                lyrics = tag.getFirst(FieldKey.LYRICS);
                if (lyrics == null || lyrics.isEmpty()) {
                    System.out.println("Failed to extract lyrics using default JaudioTagger");
                } else return lyrics;

                // --- USLT (lyrics) extraction for Jaudiotagger 3.0.1 ---
                if (tag instanceof AbstractID3v2Tag id3Tag) {
                    List<TagField> usltFields = id3Tag.getFields("USLT");
                    for (TagField field : usltFields) {
                        if (field instanceof AbstractID3v2Frame frame &&
                            frame.getBody() instanceof FrameBodyUSLT usltBody) {
                            String text = usltBody.getLyric();
                            if (text != null && !text.isEmpty()) {
                                lyrics = text;
                                break;
                            }
                        }
                    }
                }

                if (lyrics == null || lyrics.isEmpty()) {
                    System.out.println("Failed to extract lyrics using default USLT lyrics extraction");
                } else return lyrics;

                // Method 2: Try Vorbis Comment fields directly
                if (tag instanceof FlacTag flacTag) {
                    VorbisCommentTag vorbisTag = flacTag.getVorbisCommentTag();

                    if (vorbisTag != null) {
                        // Try common lyrics field names in FLAC
                        String[] lyricFields = {"LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "TEXT"};

                        for (String fieldName : lyricFields) {
                            try {
                                String extractedLyrics = vorbisTag.getFirst(fieldName);
                                if (extractedLyrics != null && !extractedLyrics.isEmpty()) {
                                    System.out.println("Extracted FLAC lyrics from field: " + fieldName);
                                    return extractedLyrics;
                                }
                            } catch (Exception e) {
                                System.out.println("Could not extract lyrics using VorbisCommentTag: " + fieldName);
                            }
                        }
                    }
                }

                //External library fallback (mp3agic)
                if (lyrics == null || lyrics.isEmpty()) {
                    try {
                        Mp3File mp3file = new Mp3File(path);
                        if (mp3file.hasId3v2Tag()) {
                            ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                            String lyrics2 = id3v2Tag.getLyrics();
                            if (lyrics2 != null && !lyrics2.isEmpty()) lyrics = lyrics2;
                        }
                    } catch (Exception e) {
                        System.out.println("mp3agic could not find lyrics");
                    }
                }

                if (lyrics == null || lyrics.isEmpty()) {
                    System.out.println("Failed to extract lyrics using mp3agic extraction");
                } else return lyrics;
            }
        }
        return lyrics;
    }

}
