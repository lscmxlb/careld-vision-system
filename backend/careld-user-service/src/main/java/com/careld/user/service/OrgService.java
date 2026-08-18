package com.careld.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.user.entity.Agent;
import com.careld.user.entity.BrandHq;
import com.careld.user.entity.OpsCenter;

import java.util.List;

public interface OrgService {

    // ===== 总部 =====
    IPage<BrandHq> listHq(String keyword, Integer page, Integer size);
    List<BrandHq> listAllHq();
    BrandHq getHqById(Long id);
    Long createHq(BrandHq hq);
    void updateHq(Long id, BrandHq hq);
    void deleteHq(Long id);

    // ===== 运营中心 =====
    IPage<OpsCenter> listCenters(Long hqId, String keyword, Integer page, Integer size);
    List<OpsCenter> listAllCenters(Long hqId);
    OpsCenter getCenterById(Long id);
    Long createCenter(OpsCenter center);
    void updateCenter(Long id, OpsCenter center);
    void deleteCenter(Long id);

    // ===== 代理商 =====
    IPage<Agent> listAgents(Long centerId, String keyword, Integer page, Integer size);
    List<Agent> listAllAgents(Long centerId);
    Agent getAgentById(Long id);
    Long createAgent(Agent agent);
    void updateAgent(Long id, Agent agent);
    void deleteAgent(Long id);
}
