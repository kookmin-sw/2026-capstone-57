package com.ilgiyebo.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 캠퍼스 노드 인덱싱 요청 DTO.
 * 백엔드 → AI 서버 HTTP POST /api/campus-nodes/index
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampusNodeIndexRequest(
    String nodeId,
    String source,
    String name,
    List<String> typeActivity,
    String description,
    String operatingHours,
    String buildingName,
    Integer floor
) {

    public static CampusNodeIndexRequest forVenue(
            String nodeId, String name, List<String> typeActivity,
            String description, String operatingHours) {
        return new CampusNodeIndexRequest(
            nodeId, "VENUE", name, typeActivity, description, operatingHours, null, null
        );
    }

    public static CampusNodeIndexRequest forBuildingPlace(
            String nodeId, String name, List<String> typeActivity,
            String description, String operatingHours, String buildingName, int floor) {
        return new CampusNodeIndexRequest(
            nodeId, "BUILDING_PLACE", name, typeActivity, description, operatingHours, buildingName, floor
        );
    }
}
