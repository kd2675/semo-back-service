package semo.back.service.common.jpa;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public abstract class CommonDateDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updateDate;

    protected CommonDateDTO() {
    }

    protected <T extends CommonDateEntity> CommonDateDTO(T t) {
        this.createDate = t.getCreateDate();
        this.updateDate = t.getUpdateDate();
    }
}
