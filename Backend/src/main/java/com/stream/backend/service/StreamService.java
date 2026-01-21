package com.stream.backend.service;

import java.util.List;
import org.springframework.data.domain.Page;
import com.stream.backend.entity.Stream;

public interface StreamService {
    Page<Stream> getStreamsByUserId(Integer userId, int page, int size, String sort);

    List<Stream> getStreamsByUserId(Integer userId);

    List<Stream> getStreamsByUserId(Integer userId, String sort);

    Stream createStream(Stream stream, Integer userId);

    void deleteStream(Stream stream);

    Stream updateStream(Integer streamId, Stream stream);
}
