package com.stream.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.stream.backend.entity.Stream;

public interface StreamService {
    List<Stream> getStreamsByChannelId(Integer channelId);

    Stream createStream(Stream stream, Integer channelId);

    void deleteStream(Stream stream);

    Stream updateStream(Integer streamId, Stream stream);

    Page<Stream> getStreamsByChannelId(Integer channelId, int page, int size, String sort);
}
