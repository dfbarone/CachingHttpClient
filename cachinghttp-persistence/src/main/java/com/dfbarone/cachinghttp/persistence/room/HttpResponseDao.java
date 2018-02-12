package com.dfbarone.cachinghttp.persistence.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

/**
 * Created by dominicbarone on 11/6/17.
 */

@Dao
public interface HttpResponseDao {

    @Query("SELECT * FROM http_response")
    List<HttpResponseEntry> selectAll();

    @Query("SELECT * FROM http_response where url LIKE  :url")
    HttpResponseEntry selectResponseByUrl(String url);

    @Query("SELECT * FROM http_response where url LIKE  :url")
    Single<HttpResponseEntry> selectResponseByUrlSingle(String url);

    @Query("SELECT * FROM http_response where url LIKE  :url")
    Flowable<HttpResponseEntry> selectResponseByUrlFlowable(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HttpResponseEntry httpResponse);

    @Delete
    void delete(HttpResponseEntry response);
}
