package com.careld.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.user.entity.Agent;
import com.careld.user.entity.BrandHq;
import com.careld.user.entity.OpsCenter;
import com.careld.user.mapper.AgentMapper;
import com.careld.user.mapper.BrandHqMapper;
import com.careld.user.mapper.OpsCenterMapper;
import com.careld.user.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final BrandHqMapper brandHqMapper;
    private final OpsCenterMapper opsCenterMapper;
    private final AgentMapper agentMapper;

    // ===== 总部 =====
    @Override
    public IPage<BrandHq> listHq(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<BrandHq> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(BrandHq::getBrandName, keyword);
        }
        wrapper.orderByDesc(BrandHq::getId);
        return brandHqMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<BrandHq> listAllHq() {
        return brandHqMapper.selectList(new LambdaQueryWrapper<BrandHq>().eq(BrandHq::getStatus, 1));
    }

    @Override
    public BrandHq getHqById(Long id) {
        BrandHq hq = brandHqMapper.selectById(id);
        if (hq == null) throw new BusinessException(404, "总部不存在");
        return hq;
    }

    @Override
    public Long createHq(BrandHq hq) {
        brandHqMapper.insert(hq);
        return hq.getId();
    }

    @Override
    public void updateHq(Long id, BrandHq hq) {
        hq.setId(id);
        brandHqMapper.updateById(hq);
    }

    @Override
    public void deleteHq(Long id) {
        brandHqMapper.deleteById(id);
    }

    // ===== 运营中心 =====
    @Override
    public IPage<OpsCenter> listCenters(Long hqId, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<OpsCenter> wrapper = new LambdaQueryWrapper<>();
        if (hqId != null) {
            wrapper.eq(OpsCenter::getHqId, hqId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(OpsCenter::getCenterName, keyword).or().like(OpsCenter::getCenterCode, keyword));
        }
        wrapper.orderByDesc(OpsCenter::getId);
        IPage<OpsCenter> result = opsCenterMapper.selectPage(new Page<>(page, size), wrapper);
        // 填充代理商数量和门店数量
        enrichCentersWithCounts(result.getRecords());
        return result;
    }

    @Override
    public List<OpsCenter> listAllCenters(Long hqId) {
        LambdaQueryWrapper<OpsCenter> wrapper = new LambdaQueryWrapper<>();
        if (hqId != null) {
            wrapper.eq(OpsCenter::getHqId, hqId);
        }
        wrapper.eq(OpsCenter::getStatus, 1);
        List<OpsCenter> centers = opsCenterMapper.selectList(wrapper);
        enrichCentersWithCounts(centers);
        return centers;
    }

    private void enrichCentersWithCounts(List<OpsCenter> centers) {
        if (centers == null || centers.isEmpty()) return;
        // 查询每个运营中心的代理商数量
        List<Agent> allAgents = agentMapper.selectList(null);
        Map<Long, List<Agent>> agentsByCenter = allAgents.stream()
                .collect(Collectors.groupingBy(Agent::getCenterId));
        // 查询每个代理商的门店数量
        Map<Long, Integer> storeCountByAgent = getStoreCountByAgent();
        for (OpsCenter center : centers) {
            List<Agent> centerAgents = agentsByCenter.getOrDefault(center.getId(), List.of());
            center.setAgentCount(centerAgents.size());
            // 门店数量 = 该运营中心下所有代理商的门店总数
            int storeCount = centerAgents.stream()
                    .mapToInt(a -> storeCountByAgent.getOrDefault(a.getId(), 0))
                    .sum();
            center.setStoreCount(storeCount);
        }
    }

    private Map<Long, Integer> getStoreCountByAgent() {
        List<Map<String, Object>> rows = agentMapper.countStoresGroupByAgent();
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r.get("agent_id")).longValue(),
                r -> ((Number) r.get("cnt")).intValue(),
                (a, b) -> a
        ));
    }

    @Override
    public OpsCenter getCenterById(Long id) {
        OpsCenter center = opsCenterMapper.selectById(id);
        if (center == null) throw new BusinessException(404, "运营中心不存在");
        return center;
    }

    @Override
    public Long createCenter(OpsCenter center) {
        opsCenterMapper.insert(center);
        return center.getId();
    }

    @Override
    public void updateCenter(Long id, OpsCenter center) {
        center.setId(id);
        opsCenterMapper.updateById(center);
    }

    @Override
    public void deleteCenter(Long id) {
        // 检查是否有代理商
        LambdaQueryWrapper<Agent> agentWrapper = new LambdaQueryWrapper<>();
        agentWrapper.eq(Agent::getCenterId, id);
        long agentCount = agentMapper.selectCount(agentWrapper);
        if (agentCount > 0) {
            throw new BusinessException(400, "该运营中心下还有" + agentCount + "个代理商，无法删除");
        }
        opsCenterMapper.deleteById(id);
    }

    // ===== 代理商 =====
    @Override
    public IPage<Agent> listAgents(Long centerId, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        if (centerId != null) {
            wrapper.eq(Agent::getCenterId, centerId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Agent::getAgentName, keyword).or().like(Agent::getAgentCode, keyword));
        }
        wrapper.orderByDesc(Agent::getId);
        IPage<Agent> result = agentMapper.selectPage(new Page<>(page, size), wrapper);
        enrichAgentsWithCounts(result.getRecords());
        return result;
    }

    @Override
    public List<Agent> listAllAgents(Long centerId) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        if (centerId != null) {
            wrapper.eq(Agent::getCenterId, centerId);
        }
        wrapper.eq(Agent::getStatus, 1);
        List<Agent> agents = agentMapper.selectList(wrapper);
        enrichAgentsWithCounts(agents);
        return agents;
    }

    private void enrichAgentsWithCounts(List<Agent> agents) {
        if (agents == null || agents.isEmpty()) return;
        // 查询运营中心名称
        List<OpsCenter> allCenters = opsCenterMapper.selectList(null);
        Map<Long, String> centerNameMap = allCenters.stream()
                .collect(Collectors.toMap(OpsCenter::getId, OpsCenter::getCenterName, (a, b) -> a));
        // 查询门店数量
        Map<Long, Integer> storeCountByAgent = getStoreCountByAgent();
        for (Agent agent : agents) {
            agent.setCenterName(centerNameMap.getOrDefault(agent.getCenterId(), "-"));
            agent.setStoreCount(storeCountByAgent.getOrDefault(agent.getId(), 0));
        }
    }

    @Override
    public Agent getAgentById(Long id) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) throw new BusinessException(404, "代理商不存在");
        return agent;
    }

    @Override
    public Long createAgent(Agent agent) {
        agentMapper.insert(agent);
        return agent.getId();
    }

    @Override
    public void updateAgent(Long id, Agent agent) {
        agent.setId(id);
        agentMapper.updateById(agent);
    }

    @Override
    public void deleteAgent(Long id) {
        // 检查是否有关联的医院
        int storeCount = agentMapper.countStoresByAgentId(id);
        if (storeCount > 0) {
            throw new BusinessException(400, "该代理商下还有" + storeCount + "家医院，无法删除");
        }
        agentMapper.deleteById(id);
    }
}
