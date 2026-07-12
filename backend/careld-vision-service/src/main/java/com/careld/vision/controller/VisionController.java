package com.careld.vision.controller;
import com.careld.common.result.Result;
import com.careld.vision.entity.VisionTestRecord;
import com.careld.vision.service.VisionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@Tag(name = "视力检测管理")
@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
public class VisionController {
    private final VisionService visionService;
    @GetMapping("/records")
    public Result<List<VisionTestRecord>> list(@RequestParam("childId") Long childId) {
        return Result.success(visionService.listByChild(childId));
    }
    @GetMapping("/records/{id}")
    public Result<VisionTestRecord> get(@PathVariable Long id) {
        return Result.success(visionService.getRecord(id));
    }
    @PostMapping("/records")
    public Result<Long> create(@RequestBody VisionTestRecord record) {
        return Result.success(visionService.createRecord(record));
    }
    @GetMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestParam("childId") Long childId, @RequestParam("reserveId") Long reserveId) {
        return Result.success(visionService.compareVision(childId, reserveId));
    }
}
