package com.dkay229.skadi.aws.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedAwsSdkS3AccessLayerTest {

    private AwsSdkS3AccessLayer mockDelegate;
    private CachedAwsSdkS3AccessLayer cachedLayer;
    private Path cacheDir;

    @BeforeEach
    void setUp() throws Exception {
        mockDelegate = mock(AwsSdkS3AccessLayer.class);
        cacheDir = Files.createTempDirectory("cache");
        cachedLayer = new CachedAwsSdkS3AccessLayer(mockDelegate, cacheDir, 1024 * 1024); // 1 MB max capacity
    }

    @Test
    void testGetBytes_CacheHit() throws Exception {
        S3Models.ObjectRef ref = new S3Models.ObjectRef("test-bucket", "test-key");
        Path cacheFile = cacheDir.resolve("test-bucket_test-key");
        byte[] cachedData = "cached-data".getBytes();
        Files.write(cacheFile, cachedData);

        byte[] result = cachedLayer.getBytes(ref);

        assertArrayEquals(cachedData, result);
        verify(mockDelegate, never()).getBytes(ref);
    }

    @Test
    void testGetBytes_CacheMiss() throws Exception {
        S3Models.ObjectRef ref = new S3Models.ObjectRef("test-bucket", "test-key");
        byte[] s3Data = "s3-data".getBytes();
        when(mockDelegate.getBytes(ref)).thenReturn(s3Data);

        byte[] result = cachedLayer.getBytes(ref);

        assertArrayEquals(s3Data, result);
        assertTrue(Files.exists(cacheDir.resolve("test-bucket_test-key")));
        verify(mockDelegate, times(1)).getBytes(ref);
    }

    @Test
    void testEviction_WhenCacheExceedsCapacity() throws Exception {
        cachedLayer = new CachedAwsSdkS3AccessLayer(mockDelegate, cacheDir, 9); // 10 bytes max capacity

        S3Models.ObjectRef ref1 = new S3Models.ObjectRef("test-bucket", "key1");
        S3Models.ObjectRef ref2 = new S3Models.ObjectRef("test-bucket", "key2");
        when(mockDelegate.getBytes(ref1)).thenReturn("data1".getBytes());
        when(mockDelegate.getBytes(ref2)).thenReturn("data2".getBytes());

        cachedLayer.getBytes(ref1); // Cache "data1"
        cachedLayer.getBytes(ref2); // Cache "data2", evict "data1"

        assertFalse(Files.exists(cacheDir.resolve("test-bucket_key1")));
        assertTrue(Files.exists(cacheDir.resolve("test-bucket_key2")));
    }

    @Test
    void testDelete_RemovesFromCache() throws Exception {
        S3Models.ObjectRef ref = new S3Models.ObjectRef("test-bucket", "test-key");
        Path cacheFile = cacheDir.resolve("test-bucket_test-key");
        Files.write(cacheFile, "cached-data".getBytes());

        cachedLayer.delete(ref);

        assertFalse(Files.exists(cacheFile));
        verify(mockDelegate, times(1)).delete(ref);
    }

    @Test
    void testDelegateMethods() {
        S3Models.ObjectRef ref = new S3Models.ObjectRef("test-bucket", "test-key");
        cachedLayer.exists(ref);
        verify(mockDelegate, times(1)).exists(ref);

        cachedLayer.head(ref);
        verify(mockDelegate, times(1)).head(ref);

        cachedLayer.putBytes(ref, "data".getBytes(), "text/plain", Map.of());
        verify(mockDelegate, times(1)).putBytes(ref, "data".getBytes(), "text/plain", Map.of());

        cachedLayer.getStream(ref);
        verify(mockDelegate, times(1)).getStream(ref);
    }
}