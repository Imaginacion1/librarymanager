package com.imaginacion.library;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


// Componente no visible para gestionar la biblioteca musical utilizando SQLite
@DesignerComponent(version = 1, description = "", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "")
@SimpleObject(external = true)
@UsesLibraries(libraries = "")
@UsesPermissions(permissionNames = "")

public class Lyralibrary extends AndroidNonvisibleComponent {
    // Variables de instancia
    private Context context;
    private Activity activity;
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    // Constructor del componente
    public Lyralibrary(ComponentContainer container) {
        super(container.$form());
        this.activity = container.$context();
        this.context = container.$context();
        dbHelper = new DBHelper(context);
    }

    // Método para obtener la base de datos SQLite
    private SQLiteDatabase getDatabase() {
        if (db == null) {
            DBHelper dbHelper = new DBHelper(this.context);
            db = dbHelper.getWritableDatabase();
        }
        return db;
    }

    // Clase interna para manejar la creación y actualización de la base de datos
    private class DBHelper extends SQLiteOpenHelper {
        // Constructor de DBHelper
        public DBHelper(Context context) {
            super(context, "mainly.db", null, 1);
        }

        // Método para crear las tablas en la base de datos
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Crear tabla tracks
            db.execSQL("CREATE TABLE IF NOT EXISTS tracks (track_id INTEGER PRIMARY KEY, track_title TEXT, track_album_id INTEGER, track_genre_id INTEGER, track_duration INTEGER, track_is_readable TEXT, track_release TEXT, track_isrc TEXT, track_album_recordtype TEXT, track_album_position INTEGER, FOREIGN KEY(track_album_id) REFERENCES albums(album_id), FOREIGN KEY(track_genre_id) REFERENCES genres(genre_id))");

            // Crear tabla albums
            db.execSQL("CREATE TABLE IF NOT EXISTS albums (album_id INTEGER PRIMARY KEY, album_name TEXT, album_release TEXT)");

            // Crear tabla artists
            db.execSQL("CREATE TABLE IF NOT EXISTS artists (artist_id INTEGER PRIMARY KEY, artist_name TEXT)");

            // Crear tabla genres
            db.execSQL("CREATE TABLE IF NOT EXISTS genres (genre_id INTEGER PRIMARY KEY, genre_name TEXT)");

            // Crear tabla playlists
            db.execSQL("CREATE TABLE IF NOT EXISTS playlists (playlist_id INTEGER PRIMARY KEY, playlist_name TEXT, playlist_created TEXT)");

            // Crear tabla playlist_tracks
            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_tracks (playlist_id INTEGER, track_id INTEGER, PRIMARY KEY(playlist_id, track_id), FOREIGN KEY(playlist_id) REFERENCES playlists(playlist_id), FOREIGN KEY(track_id) REFERENCES tracks(track_id))");

            // Crear tabla track_artists
            db.execSQL("CREATE TABLE IF NOT EXISTS track_artists (track_id INTEGER, artist_id INTEGER, PRIMARY KEY(track_id, artist_id), FOREIGN KEY(track_id) REFERENCES tracks(track_id), FOREIGN KEY(artist_id) REFERENCES artists(artist_id))");

            // Crear tabla genre_tracks
            db.execSQL("CREATE TABLE IF NOT EXISTS genre_tracks (genre_id INTEGER, track_id INTEGER, PRIMARY KEY(genre_id, track_id), FOREIGN KEY(genre_id) REFERENCES genres(genre_id), FOREIGN KEY(track_id) REFERENCES tracks(track_id))");

            // Crear tabla album_tracks
            db.execSQL("CREATE TABLE IF NOT EXISTS album_tracks (album_id INTEGER, track_id INTEGER, track_album_position INTEGER, PRIMARY KEY(album_id, track_id), FOREIGN KEY(album_id) REFERENCES albums(album_id), FOREIGN KEY(track_id) REFERENCES tracks(track_id))");

            // Crear tabla album_artists
            db.execSQL("CREATE TABLE IF NOT EXISTS album_artists (album_id INTEGER, artist_id INTEGER, PRIMARY KEY(album_id, artist_id), FOREIGN KEY(album_id) REFERENCES albums(album_id), FOREIGN KEY(artist_id) REFERENCES artists(artist_id))");

            // Crear índices para optimizar las consultas
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_track_id ON tracks(track_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_track_title ON tracks(track_title)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_album_id ON albums(album_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_album_name ON albums(album_name)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_artist_id ON artists(artist_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_artist_name ON artists(artist_name)");
        }
        // Método para manejar la actualización de la base de datos
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
    
    // Función para eliminar la base de datos y volverla a crear
    @SimpleFunction(description = "Delete base de datos")
    public void resetDatabase() {
        // Cerrar la base de datos si está abierta
        if (db != null && db.isOpen()) {
            db.close();
        }
        // Eliminar el archivo de la base de datos
        context.deleteDatabase("mainly.db");
        // Crear la base de datos y abrirla
        DBHelper dbHelper = new DBHelper(context);
        db = dbHelper.getWritableDatabase();
    }
    
    // Evento unificado para manejar operaciones en la librería
    @SimpleEvent(description = "Evento unificado para manejar operaciones en la librería")
    public void LibraryEvent(String eventCode, String eventDescription, long objectId1, long objectId2) {
        Object[] eventData = new Object[4];
        eventData[0] = eventCode;
        eventData[1] = eventDescription;
        eventData[2] = objectId1;
        eventData[3] = objectId2 != -1 ? objectId2 : "";
        EventDispatcher.dispatchEvent(this, "LibraryEvent", eventData);
    }

    @SimpleFunction(description = "Obtener todas las filas de una tabla específica")
    public YailList ObtenerFilasDeTabla(String nombreTabla) {
        SQLiteDatabase db = getDatabase();
    
        Cursor cursor = db.rawQuery("SELECT * FROM " + nombreTabla, null);
        List<List<Object>> tablaCompleta = new ArrayList<>();
    
        if (cursor.moveToFirst()) {
            do {
                List<Object> fila = new ArrayList<>();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            fila.add(cursor.getLong(i)); // Cambiado de getInt a getLong
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            fila.add(cursor.getFloat(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            fila.add(cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            fila.add(cursor.getBlob(i));
                            break;
                        case Cursor.FIELD_TYPE_NULL:
                            fila.add(null);
                            break;
                    }
                }
                tablaCompleta.add(fila);
            } while (cursor.moveToNext());
        }
    
        cursor.close();
        return YailList.makeList(tablaCompleta);
    }
    
    @SimpleFunction(description = "Obtener información de diferentes tablas según el itemType y itemIdInput")
    public YailList obtenerInformacion(String itemType, String itemFilter, long itemId) {
        SQLiteDatabase db = getDatabase();

        // Construcción de la consulta principal
        String query = "";
        String[] queryArgs = null;

        switch (itemType) {
            case "tracks":
                if (itemFilter.isEmpty()) {
                    query = "SELECT * FROM tracks";
                } else if (itemFilter.equals("album_id")) { 
                    query = "SELECT * FROM tracks WHERE track_album_id = ?";
                    queryArgs = new String[]{String.valueOf(itemId)};
                } else if (itemFilter.equals("artist_id")) {
                    query = "SELECT tracks.* FROM track_artists INNER JOIN tracks ON track_artists.track_id = tracks.track_id WHERE track_artists.artist_id = ?";
                    queryArgs = new String[]{String.valueOf(itemId)};
                } else if (itemFilter.equals("genre_id")) {
                    query = "SELECT tracks.* FROM genre_tracks INNER JOIN tracks ON genre_tracks.track_id = tracks.track_id WHERE genre_tracks.genre_id = ?";
                    queryArgs = new String[]{String.valueOf(itemId)};
                } else if (itemFilter.equals("playlist_id")) {
                    query = "SELECT tracks.* FROM playlist_tracks INNER JOIN tracks ON playlist_tracks.track_id = tracks.track_id WHERE playlist_tracks.playlist_id = ?";
                    queryArgs = new String[]{String.valueOf(itemId)};
                }
                break;
            case "albums":
                if (itemFilter.isEmpty()) {
                    query = "SELECT * FROM albums";
                } else if (itemFilter.equals("artist_id")) {
                    query = "SELECT albums.* FROM album_artists INNER JOIN albums ON album_artists.album_id = albums.album_id WHERE album_artists.artist_id = ?";
                    queryArgs = new String[]{String.valueOf(itemId)};
                }
                break;
            case "artists":
                query = "SELECT * FROM artists";
                break;
            case "genres":
                query = "SELECT * FROM genres";
                break;
            case "playlists":
                query = "SELECT * FROM playlists";
                break;
        }

        // Ejecución de la consulta principal
        Cursor cursor = db.rawQuery(query, queryArgs);

        // Procesamiento de los resultados de la consulta principal
        JSONArray jsonArray = new JSONArray();
        while (cursor.moveToNext()) {
            JSONObject jsonObject = new JSONObject();

            try {
                switch (itemType) {
                    case "tracks":
                        jsonObject.put("track_id", cursor.getLong(cursor.getColumnIndex("track_id")));
                        jsonObject.put("track_title", cursor.getString(cursor.getColumnIndex("track_title")));
                        jsonObject.put("track_duration", cursor.getLong(cursor.getColumnIndex("track_duration")));

                        if (itemFilter.equals("album_id")) {
                            jsonObject.put("track_release", cursor.getString(cursor.getColumnIndex("track_release")));
                            jsonObject.put("track_album_position", cursor.getLong(cursor.getColumnIndex("track_album_position")));
                        } else if (itemFilter.isEmpty() || itemFilter.equals("artist_id") || itemFilter.equals("genre_id") || itemFilter.equals("playlist_id")) {
                            JSONArray artistNames = new JSONArray();
                            long trackId = cursor.getLong(cursor.getColumnIndex("track_id"));
                            Cursor artistCursor = db.rawQuery("SELECT artists.artist_name FROM track_artists INNER JOIN artists ON track_artists.artist_id = artists.artist_id WHERE track_artists.track_id = ?", new String[]{String.valueOf(trackId)});
                            while (artistCursor.moveToNext()) {
                                artistNames.put(artistCursor.getString(artistCursor.getColumnIndex("artist_name")));
                            }
                            jsonObject.put("track_artist_names", artistNames);
                            artistCursor.close();
                        }
                        break;
                    case "albums":
                        jsonObject.put("album_id", cursor.getLong(cursor.getColumnIndex("album_id")));
                        jsonObject.put("album_name", cursor.getString(cursor.getColumnIndex("album_name")));
                        jsonObject.put("album_release", cursor.getString(cursor.getColumnIndex("album_release")));
                        break;
                    case "artists":
                        jsonObject.put("artist_id", cursor.getLong(cursor.getColumnIndex("artist_id")));
                        jsonObject.put("artist_name", cursor.getString(cursor.getColumnIndex("artist_name")));
                        break;
                    case "genres":
                        jsonObject.put("genre_id", cursor.getLong(cursor.getColumnIndex("genre_id")));
                        jsonObject.put("genre_name", cursor.getString(cursor.getColumnIndex("genre_name")));
                        break;
                    case "playlists":
                        jsonObject.put("playlist_id", cursor.getLong(cursor.getColumnIndex("playlist_id")));
                        jsonObject.put("playlist_name", cursor.getString(cursor.getColumnIndex("playlist_name")));
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            jsonArray.put(jsonObject);
        }
        cursor.close();
        // Se devuelve la lista de objetos JSON como una YailList para ser utilizada en App Inventor.
        List<Object> jsonList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                jsonList.add(jsonArray.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return YailList.makeList(jsonList);
    }

    @SimpleFunction(description = "Eliminar canción de la base de datos utilizando el ID")
    public void deletetrack(long id) {
    SQLiteDatabase db = getDatabase();
    db.beginTransaction();
        try {
            // Paso 1 y 5: Eliminar el álbum al que pertenece la canción solo y solo si este álbum no pertenece a ningún otro track
            String albumIdQuery = "SELECT track_album_id FROM tracks WHERE track_id = ?";
            Cursor albumIdCursor = db.rawQuery(albumIdQuery, new String[]{String.valueOf(id)});
            if (albumIdCursor.moveToFirst()) {
                long albumId = albumIdCursor.getLong(0);
                String checkAlbumQuery = "SELECT track_id FROM tracks WHERE track_album_id = ? AND track_id != ?";
                Cursor checkAlbumCursor = db.rawQuery(checkAlbumQuery, new String[]{String.valueOf(albumId), String.valueOf(id)});
                if (!checkAlbumCursor.moveToFirst()) {
                    db.delete("albums", "album_id = ?", new String[]{String.valueOf(albumId)});
                    // Informar que se elimino el album
                    LibraryEvent("event_album_removed", "Album eliminado correctamente", albumId, -1L);                    
                }
                db.delete("album_tracks", "track_id = ?", new String[]{String.valueOf(id)});
            }
            albumIdCursor.close();

            // Paso 2 y 5: Eliminar el artista al que pertenece la canción solo y solo si este artista no pertenece a ningún otro track
            String artistIdQuery = "SELECT artist_id FROM track_artists WHERE track_id = ?";
            Cursor artistIdCursor = db.rawQuery(artistIdQuery, new String[]{String.valueOf(id)});
            while (artistIdCursor.moveToNext()) {
                long artistId = artistIdCursor.getLong(0);
                String checkArtistQuery = "SELECT track_id FROM track_artists WHERE artist_id = ? AND track_id != ?";
                Cursor checkArtistCursor = db.rawQuery(checkArtistQuery, new String[]{String.valueOf(artistId), String.valueOf(id)});
                if (!checkArtistCursor.moveToFirst()) {
                    db.delete("artists", "artist_id = ?", new String[]{String.valueOf(artistId)});
                    // Después de eliminar el artista
                    LibraryEvent("event_artist_removed", "Artista eliminado correctamente", artistId, -1L);                    
                }
                db.delete("track_artists", "track_id = ?", new String[]{String.valueOf(id)});
            }
            artistIdCursor.close();

            // Paso 3 y 5: Eliminar el playlist al que pertenece la canción solo y solo si este playlist no pertenece a ningún otro track
            String playlistIdQuery = "SELECT playlist_id FROM playlist_tracks WHERE track_id = ?";
            Cursor playlistIdCursor = db.rawQuery(playlistIdQuery, new String[]{String.valueOf(id)});
            while (playlistIdCursor.moveToNext()) {
                long playlistId = playlistIdCursor.getLong(0);
                String checkPlaylistQuery = "SELECT track_id FROM playlist_tracks WHERE playlist_id = ? AND track_id != ?";
                Cursor checkPlaylistCursor = db.rawQuery(checkPlaylistQuery, new String[]{String.valueOf(playlistId), String.valueOf(id)});
                if (!checkPlaylistCursor.moveToFirst()) {
                    db.delete("playlists", "playlist_id = ?", new String[]{String.valueOf(playlistId)});
                    // Después de eliminar la playlist
                    LibraryEvent("event_playlist_removed", "PLaylist eliminado correctamente", playlistId, -1L);                    
                }
                db.delete("playlist_tracks", "track_id = ?", new String[]{String.valueOf(id)});
            }
            playlistIdCursor.close();

            // Paso 4 y 5: Eliminar la canción de la tabla "tracks"
            db.delete("tracks", "track_id = ?", new String[]{String.valueOf(id)});
            LibraryEvent("event_track_removed", "Cancion eliminada correctamente", id, -1L);       

            // Eliminar relacion de la tabla "genre_tracks"
            db.delete("genre_tracks", "track_id = ?", new String[]{String.valueOf(id)});

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @SimpleFunction(description = "Administrar playlists)")
    public void playlistManager(String action, String playlistName, long playlistId, long trackId) {
        SQLiteDatabase db = getDatabase(); // Obtiene la base de datos
    
        // Procesar la acción según el valor de "action"
        switch (action) {
            // Crear una nueva playlist
            case "create_playlist":
            dbHelper.onCreate(db);
            // Preparar los valores a insertar en la tabla "playlists"
            ContentValues playlistValues = new ContentValues();
            playlistValues.put("playlist_name", playlistName);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            playlistValues.put("playlist_created", dateFormat.format(new Date()));
        
            // Insertar la nueva playlist en la base de datos
            db.insert("playlists", null, playlistValues);
        
            // Obtener el último playlist_id insertado en la tabla "playlists"
            String query = "SELECT MAX(playlist_id) FROM playlists";
            Cursor cursor = db.rawQuery(query, null);
            long newPlaylistId = -1;
            if (cursor != null && cursor.moveToFirst()) {
                newPlaylistId = cursor.getLong(0);
                cursor.close();
            }
        
            // Disparar el evento PlaylistCreated
            LibraryEvent("event_li_playlist_created", "Playlist agregada correctamente", newPlaylistId, -1L);
            break;
            // Agregar una canción a una playlist
            case "add_track":
 
               // Preparar los valores a insertar en la tabla "playlist_tracks"
                ContentValues playlistTrackValues = new ContentValues();
                playlistTrackValues.put("playlist_id", playlistId);
                playlistTrackValues.put("track_id", trackId);

                // Insertar la relación entre la playlist y la canción en la base de datos
                db.insert("playlist_tracks", null, playlistTrackValues);

                // Disparar el evento TrackAdded
                LibraryEvent("event_li_playlist_track_added", "Cancion agregada correctamente a la Playlist", playlistId, trackId);
                break;

            // Eliminar una canción de una playlist
            case "remove_track":

                // Preparar la cláusula WHERE y los argumentos para eliminar la relación entre la playlist y la canción
                String removeTrackWhereClause = "playlist_id = ? AND track_id = ?";
                String[] removeTrackArgs = new String[]{String.valueOf(playlistId), String.valueOf(trackId)};

                // Eliminar la relación entre la playlist y la canción de la base de datos
                db.delete("playlist_tracks", removeTrackWhereClause, removeTrackArgs);

                // Disparar el evento TrackRemoved
                LibraryEvent("event_li_playlist_track_removed", "Cancion eliminada correctamente de la Playlist", playlistId, trackId);
                break;

            // Eliminar una playlist
            case "delete_playlist":

                // Preparar la cláusula WHERE y los argumentos para eliminar las relaciones entre la playlist y sus canciones
                String deletePlaylistTracksWhereClause = "playlist_id = ?";
                String[] deletePlaylistTracksArgs = new String[]{String.valueOf(playlistId)};

                // Eliminar las relaciones entre la playlist y sus canciones de la base de datos
                db.delete("playlist_tracks", deletePlaylistTracksWhereClause, deletePlaylistTracksArgs);

                // Preparar la cláusula WHERE y los argumentos para eliminar la playlist
                String deletePlaylistWhereClause = "playlist_id = ?";
                String[] deletePlaylistArgs = new String[]{String.valueOf(playlistId)};

                // Eliminar la playlist de la base de datos
                db.delete("playlists", deletePlaylistWhereClause, deletePlaylistArgs);

                // Disparar el evento PlaylistRemoved
                LibraryEvent("event_li_playlist_removed", "Playlist eliminada correctamente", playlistId, -1L);
                break;
        }
    }    

    @SimpleFunction(description = "Agregar una canción a la tabla 'tracks'")
    public void AgregarCancion(long track_id, String track_title, long track_album_id, long track_genre_id, YailList track_artists_ids, YailList track_artists_name, long track_duration, String track_is_readable, String track_release, String track_isrc, String track_album_recordtype, String track_album_name, long track_album_artist_id, long track_album_position) {
        try {
            SQLiteDatabase db = getDatabase();
            dbHelper.onCreate(db);
            db.beginTransaction(); // Iniciar la transacción
    
            // Verificar si el track_id ya existe en la tabla 'tracks'
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM tracks WHERE track_id=?", new String[] { Long.toString(track_id) });
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
    
            // Si el track_id ya existe, lanzar una excepción
            if (count > 0) {
                throw new Exception("El id de la canción ya existe");
            }
    
            // Verificar si el género existe en la tabla 'genres'
            cursor = db.rawQuery("SELECT COUNT(*) FROM genres WHERE genre_id=?", new String[]{String.valueOf(track_genre_id)});
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
    
            // Si el género no existe, insertarlo en la tabla 'genres'
            if (count == 0) {
                ContentValues genreValues = new ContentValues();
                genreValues.put("genre_id", track_genre_id);
                genreValues.put("genre_name", "none");
                db.insert("genres", null, genreValues);
                LibraryEvent("event_li_genre_added", "Nuevo genereo insertado", track_genre_id, -1L);
            }
    
            // Verificar si el álbum existe en la tabla 'albums'
            if (!track_album_recordtype.equals("single")) {
                cursor = db.rawQuery("SELECT COUNT(*) FROM albums WHERE album_id=?", new String[] { Long.toString(track_album_id) });
                cursor.moveToFirst();
                count = cursor.getInt(0);
                cursor.close();
    
                // Si el álbum no existe, insertarlo en la tabla 'albums'
                if (count == 0) {
                    ContentValues albumValues = new ContentValues();
                    albumValues.put("album_id", track_album_id);
                    albumValues.put("album_name", track_album_name);
                    albumValues.put("album_release", track_release);
                    db.insert("albums", null, albumValues);
                    LibraryEvent("event_li_album_added", "Album insertado correctamente", track_album_id, -1L);
                }
            }
    
            ContentValues values = new ContentValues();
            values.put("track_id", track_id);
            values.put("track_title", track_title);
            values.put("track_album_id", track_album_id);
            values.put("track_genre_id", track_genre_id);
            values.put("track_duration", track_duration);
            values.put("track_is_readable", track_is_readable);
            values.put("track_release", track_release);
            values.put("track_isrc", track_isrc);
            values.put("track_album_recordtype", track_album_recordtype);
            values.put("track_album_position", track_album_position); // Nuevo campo agregado
    
            if (track_album_recordtype.equals("single")) {
                track_album_id = -1L;
                track_album_position = -1L;
            }
            
    
            db.insert("tracks", null, values);
            LibraryEvent("event_li_track_added", "Cancion insertada correctamente", track_id, -1L);
    
            // Insertar relaciones en la tabla 'track_artists'
            // Eliminar las relaciones existentes entre el track y los artistas
            db.delete("track_artists", "track_id=?", new String[] { Long.toString(track_id) });
            for (int i = 0; i < track_artists_ids.size(); i++) {
                long artist_id = Long.parseLong(track_artists_ids.getString(i));
                String artist_name = track_artists_name.getString(i);
    
                // Comprobar si el artista existe en la tabla 'artists'
                cursor = db.rawQuery("SELECT COUNT(*) FROM artists WHERE artist_id=?", new String[]{Long.toString(artist_id)});
                cursor.moveToFirst();
                count = cursor.getInt(0);
                cursor.close();
    
                // Si el artista no existe, insertarlo en la tabla 'artists'
                if (count == 0) {
                    ContentValues artistValues = new ContentValues();
                    artistValues.put("artist_id", artist_id);
                    artistValues.put("artist_name", artist_name);
                    db.insert("artists", null, artistValues);
                    LibraryEvent("event_li_artist_added", "Artista insertado correctamente", artist_id, -1L);
                }
    
                // Insertar la relación en la tabla 'track_artists'
                ContentValues trackArtistValues = new ContentValues();
                trackArtistValues.put("track_id", track_id);
                trackArtistValues.put("artist_id", artist_id);
                db.insert("track_artists", null, trackArtistValues);
            }
    
            // Añadir relación en la tabla 'genre_tracks'
            ContentValues genreTrackValues = new ContentValues();
            genreTrackValues.put("track_id", track_id);
            genreTrackValues.put("genre_id", track_genre_id);
            db.insert("genre_tracks", null, genreTrackValues);
    
            // Añadir relación en la tabla 'album_tracks'
            if (!track_album_recordtype.equals("single")) {
                ContentValues albumTrackValues = new ContentValues();
                albumTrackValues.put("track_id", track_id);
                albumTrackValues.put("album_id", track_album_id);
                albumTrackValues.put("track_album_position", track_album_position); // Nuevo campo agregado
                db.insert("album_tracks", null, albumTrackValues);
            }
            // Insertar relaciones en la tabla 'album_artists' si el álbum no existe
            if (!track_album_recordtype.equals("single")) {
                if (count == 0) {
                    ContentValues albumArtistValues = new ContentValues();
                    albumArtistValues.put("album_id", track_album_id);
                    albumArtistValues.put("artist_id", track_album_artist_id);
                    db.insert("album_artists", null, albumArtistValues);
                }
            }    
            db.setTransactionSuccessful(); // Marcar la transacción como exitosa
        } catch (Exception e) {
            LibraryEvent("event_li_track_error", e.getMessage(), -1L, -1L);
        } finally {
            db.endTransaction(); // Finalizar la transacción
        }
    }

    private List<JSONObject> songDataList = null;
    private Map<String, Integer> conteoArtistas = new HashMap<>();
        
    @SimpleFunction(description = "Buscar canciones en la base de datos")
    public YailList buscarCanciones(String cadenaBusqueda, int pagina, int resultadosPorPagina, boolean recreateData) {
        if (songDataList == null || recreateData) {
            SQLiteDatabase db = getDatabase();
    
            String query = "SELECT DISTINCT tracks.track_id, tracks.track_title " +
                    "FROM tracks ";
    
            Cursor cursor = db.rawQuery(query, null);
    
            songDataList = new ArrayList<>();
            conteoArtistas.clear();
    
            while (cursor.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                try {
                    Long trackId = cursor.getLong(cursor.getColumnIndex("track_id"));
                    jsonObject.put("track_id", trackId);
                    jsonObject.put("track_title", cursor.getString(cursor.getColumnIndex("track_title")));
    
                    // Obtener los artistas asociados a la canción
                    String artistQuery = "SELECT artists.artist_name " +
                            "FROM track_artists " +
                            "INNER JOIN artists ON track_artists.artist_id = artists.artist_id " +
                            "WHERE track_artists.track_id = ?";
                    Cursor artistCursor = db.rawQuery(artistQuery, new String[]{String.valueOf(trackId)});
    
                    StringBuilder artistNames = new StringBuilder();
                    while (artistCursor.moveToNext()) {
                        String artistName = artistCursor.getString(artistCursor.getColumnIndex("artist_name"));
                        artistNames.append(artistName).append(", ");
    
                        // Actualizar conteo de artistas
                        if (conteoArtistas.containsKey(artistName)) {
                            conteoArtistas.put(artistName, conteoArtistas.get(artistName) + 1);
                        } else {
                            conteoArtistas.put(artistName, 1);
                        }
                    }
                    artistCursor.close();
    
                    if (artistNames.length() > 0) {
                        artistNames.setLength(artistNames.length() - 2); // Eliminar la última coma y espacio
                    }
                    jsonObject.put("track_artist_names", artistNames.toString());
    
                    songDataList.add(jsonObject);
    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    
        List<JSONObject> searchResults = new ArrayList<>();
        String cadenaBusquedaLower = cadenaBusqueda.toLowerCase();
        boolean shortSearchTerm = cadenaBusquedaLower.length() < 4;
    
        for (JSONObject songData : songDataList) {
            String trackTitleLower = songData.optString("track_title").toLowerCase();
            String artistNamesLower = songData.optString("track_artist_names").toLowerCase();
    
            if (shortSearchTerm) {
                if (trackTitleLower.startsWith(cadenaBusquedaLower) || artistNamesLower.startsWith(cadenaBusquedaLower)) {
                    searchResults.add(songData);
                }
            } else {
                String[] artistNames = artistNamesLower.split(", ");
                boolean artistMatch = false;
                for (String artistName : artistNames) {
                    if (artistName.contains(cadenaBusquedaLower)) {
                        artistMatch = true;
                        break;
                    }
                }
    
                String[] trackTitleWords = trackTitleLower.split(" ");
                boolean trackMatch = false;
                for (String word : trackTitleWords) {
                    if (word.contains(cadenaBusquedaLower)) {
                        trackMatch = true;
                        break;
                    }
                }
    
                if (trackMatch || artistMatch) {
                    searchResults.add(songData);
                }
            }
        }
    
        // Ordenar la lista de resultados por la cantidad de veces que aparece cada artista
        Collections.sort(searchResults, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject jsonObject1, JSONObject jsonObject2) {
                String artistName1 = jsonObject1.optString("track_artist_names");
                String artistName2 = jsonObject2.optString("track_artist_names");
                Integer count1 = conteoArtistas.get(artistName1);
                Integer count2 = conteoArtistas.get(artistName2);
    
                if (count1 == null) {
                    count1 = 0;
                }
    
                if (count2 == null) {
                    count2 = 0;
                }
    
                if (count1 > count2) {
                    return -1;
                } else if (count1 < count2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    
        // Aplicar paginación
        int startIndex = (pagina - 1) * resultadosPorPagina;
        int endIndex = Math.min(startIndex + resultadosPorPagina, searchResults.size());
        List<JSONObject> paginatedResults = searchResults.subList(startIndex, endIndex);
    
        // Crear una lista de Yail a partir de los resultados paginados
        List<Object> yailResultList = new ArrayList<>();
        for (JSONObject jsonObject : paginatedResults) {
            yailResultList.add(jsonObject);
        }
        YailList yailList = YailList.makeList(yailResultList);
        return yailList;
    }
}
