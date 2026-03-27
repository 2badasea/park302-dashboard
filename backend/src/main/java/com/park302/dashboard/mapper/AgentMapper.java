package com.park302.dashboard.mapper;

import com.park302.dashboard.dto.AgentDTO;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.repository.projection.AgentListProjection;
import org.mapstruct.Mapper;

/**
 * Agent MapStruct 매퍼
 * componentModel = "spring": Spring Bean으로 등록되어 @Autowired / 생성자 주입 가능
 */
@Mapper(componentModel = "spring")
public interface AgentMapper {

    /**
     * Agent 엔티티 → DetailResponse record 변환
     * isVisible 등 record에 없는 필드는 자동 무시됨
     */
    AgentDTO.DetailResponse toDetailResponse(Agent agent);

    /**
     * AgentListProjection 인터페이스 → ListItem record 변환
     * Projection getter 이름(getId, getName ...)과 record 컴포넌트 이름이 일치하므로 자동 매핑
     */
    AgentDTO.ListItem toListItem(AgentListProjection projection);
}
