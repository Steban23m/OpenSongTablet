package com.garethevans.church.opensongtablet;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

// This class is called asynchronously
public class IndexSongs extends Activity {

    static File searchindexlog;

    public static void doIndex(Context c) throws XmlPullParserException, IOException {
        FullscreenActivity.safetosearch = false;
        FullscreenActivity.search_database = null;
        FullscreenActivity.search_database = new ArrayList<>();
        System.gc();

        // Prepare a blank log file to show the search index progress
        searchindexlog = new File(FullscreenActivity.homedir+"/searchindexlog.txt");

        String defaultlogtext = "Search index progress.\n\n" +
                "If the last song shown in this list is not the last song in your directory, there was an error indexing it.\n" +
                "Please manually check that the file is a correctly formatted OpenSong file.\n\n\n";

        FileOutputStream overWrite = new FileOutputStream(searchindexlog, false);
        overWrite.write(defaultlogtext.getBytes());
        overWrite.flush();
        overWrite.close();

        // Get all the folders that are available
        File songfolder = new File(FullscreenActivity.dir.getAbsolutePath());
        File[] tempmyitems = null;
        if (songfolder.isDirectory()) {
            tempmyitems = songfolder.listFiles();
        }
        // Need to add MAIN folder still......
        ArrayList<File> fixedfolders = new ArrayList<>();
        fixedfolders.add(FullscreenActivity.dir);
        for (File temp:tempmyitems) {
            if (temp.isDirectory()) {
                fixedfolders.add(temp);
            }
        }

        // Prepare the xml pull parser
        XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
        xppf.setNamespaceAware(true);
        XmlPullParser xpp = xppf.newPullParser();
        String filename;
        String folder;
        String title;
        String author;
        String lyrics;
        String theme;
        String key;
        String hymnnumber;

        // Now go through each folder and load each song in turn and then add it to the array
        for (File currfolder : fixedfolders) {
            // Removes start bit for subfolders
            String foldername = currfolder.toString().replace(songfolder.toString()+"/", "");
            // If in the main folder
            if (foldername.equals(songfolder.toString())) {
                foldername = FullscreenActivity.mainfoldername;
            }
            File files[] = currfolder.listFiles();
            // Go through each file
            for (File file : files) {
                if (file.isFile() && file.exists() && file.canRead()) {
                    filename = file.toString().replace(currfolder.toString() + "/", "");
                    folder = foldername;
                    author = "";
                    lyrics = "";
                    theme = "";
                    key = "";
                    hymnnumber = "";

                    // Set the title as the filename by default in case this isn't an OpenSong xml
                    title = filename;

                    // Try to get the file length
                    long filesize;
                    try {
                        filesize = file.length();
                        filesize = filesize/1024;
                    } catch (Exception e) {
                        filesize = 1000000;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    xpp.setInput(fis, null);


                    // Extract the title, author, key, lyrics, theme
                    int eventType = xpp.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.getName().equals("author")) {
                                author = xpp.nextText();
                            } else if (xpp.getName().equals("title")) {
                                title = xpp.nextText();
                            } else if (xpp.getName().equals("lyrics")) {
                                lyrics = xpp.nextText();
                            } else if (xpp.getName().equals("key")) {
                                key = xpp.nextText();
                            } else if (xpp.getName().equals("theme")) {
                                theme = xpp.nextText();
                            } else if (xpp.getName().equals("hymn_number")) {
                                hymnnumber = xpp.nextText();
                            }
                        }
                        try {
                            eventType = xpp.next();
                        } catch (Exception e) {
                            eventType = XmlPullParser.END_DOCUMENT;
                            // This wasn't an xml, so grab the file contents instead
                            // By default, make the lyrics the content, unless it is a pdf, image, etc.
                            if (filesize<250 &&
                                    !filename.contains(".pdf") && !filename.contains(".PDF") &&
                                    !filename.contains(".doc") && !filename.contains(".DOC") &&
                                    !filename.contains(".docx") && !filename.contains(".DOCX") &&
                                    !filename.contains(".png") && !filename.contains(".PNG") &&
                                    !filename.contains(".jpg") && !filename.contains(".JPG") &&
                                    !filename.contains(".gif") && !filename.contains(".GIF") &&
                                    !filename.contains(".jpeg") && !filename.contains(".JPEG")) {
                                FileInputStream grabFileContents = new FileInputStream(file);
                                lyrics = LoadXML.readTextFile(grabFileContents);
                            }
                        }
                    }

                    // Remove chord lines, empty lines and setions in lyrics (to save memory) - only line that start with " "
                    String lyricslines[] = lyrics.split("\n");
                    String shortlyrics = "";
                    for (String line : lyricslines) {
                        if (!line.startsWith(".") && !line.startsWith("[") && !line.equals("")) {
                            if (line.startsWith(";")) {
                                line = line.substring(1);
                            }
                            shortlyrics = shortlyrics + line;
                        }
                    }
                    shortlyrics = shortlyrics.trim();

                    // Replace unwanted symbols
                    shortlyrics = ProcessSong.removeUnwantedSymbolsAndSpaces(shortlyrics);

                    shortlyrics = filename.trim() + " " + folder.trim() + " " + title.trim() + " " + author.trim() + " " +
                            c.getString(R.string.edit_song_key) + " " + key.trim() + " " + theme.trim() + " " + hymnnumber.trim() + " " + shortlyrics;

                    String item_to_add = filename + " _%%%_ " + folder + " _%%%_ " + title + " _%%%_ " + author + " _%%%_ " + shortlyrics + " _%%%_ " +
                            theme + " _%%%_ " + key + " _%%%_ " + hymnnumber;
                    FullscreenActivity.search_database.add(item_to_add);

                    String line_to_add = folder + "/" + filename+"\n";

                    //OutputStreamWriter logoutput = new OutputStreamWriter(c.openFileOutput(searchindexlog.toString(), Context.MODE_APPEND));
                    FileOutputStream logoutput = new FileOutputStream (new File(searchindexlog.getAbsolutePath()), true); // true will be same as Context.MODE_APPEND
                    logoutput.write(line_to_add.getBytes());
                    //logoutput.write(item_to_add.getBytes());
                    logoutput.flush();
                    logoutput.close();
                }
            }
        }
        FileOutputStream logoutput = new FileOutputStream (new File(searchindexlog.getAbsolutePath()), true); // true will be same as Context.MODE_APPEND
        int totalsongsindexed = FullscreenActivity.search_database.size();
        int totalsongsfound = FullscreenActivity.allfilesforsearch.size();
        String status;
        if (totalsongsfound==totalsongsindexed) {
            status = "No errors found";
        } else {
            status = "Something is wrong with the last file listed";
        }

        String extra = "\n\nTotal songs found = "+totalsongsfound+"\nTotal songs indexed="+totalsongsindexed+"\n\n"+status;
        logoutput.write(extra.getBytes());
        logoutput.flush();
        logoutput.close();

        System.gc();
        FullscreenActivity.safetosearch = true;
    }

    public static void ListAllSongs() {
        // get a list of all song subfolders
        ArrayList<String> allsongfolders = new ArrayList<>();
        ArrayList<String> allsongsinfolders = new ArrayList<>();

        // Add the MAIN folder
        allsongfolders.add(FullscreenActivity.dir.toString());

        File[] songsubfolders = FullscreenActivity.dir.listFiles();
        for (File isfolder:songsubfolders) {
            if (isfolder.isDirectory()) {
                allsongfolders.add(isfolder.toString());
            }
        }

        // Now we have all the directories, iterate through them adding each song
        for (String thisfolder:allsongfolders) {
            File currentfolder = new File(thisfolder);
            File[] thesesongs = currentfolder.listFiles();
            for (File thissong:thesesongs) {
                if (thissong.isFile()) {
                    String simplesong = thissong.toString().replace(FullscreenActivity.dir.toString()+"/","");
                    simplesong = simplesong.replace(FullscreenActivity.dir.toString(),"");
                    if (!simplesong.contains("/")) {
                        simplesong = "/" + simplesong;
                    }
                    allsongsinfolders.add(simplesong);
                }
            }
        }

        // Add this list to the main array
        FullscreenActivity.allfilesforsearch = allsongsinfolders;

    }
}