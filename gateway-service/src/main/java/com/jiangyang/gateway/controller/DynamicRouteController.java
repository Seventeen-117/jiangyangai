package com.jiangyang.gateway.controller;

import com.jiangyang.gateway.service.DynamicRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态路由管理控制器
 * 提供Spring Cloud Gateway动态路由管理接口
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/gateway/routes")
@Tag(name = "动态路由管理", description = "Spring Cloud Gateway动态路由管理接口")
@ConditionalOnProperty(name = "gateway.dynamic-routes.enabled", havingValue = "true", matchIfMissing = false)
public class DynamicRouteController {

    @Autowired
    private DynamicRouteService dynamicRouteService;

    @PostMapping
    @Operation(summary = "添加路由", description = "添加单个路由定义")
    @ApiResponse(responseCode = "201", description = "添加成功")
    @ApiResponse(responseCode = "500", description = "添加失败")
    public Mono<ResponseEntity<Void>> add(
            @Parameter(description = "路由定义", required = true) 
            @RequestBody RouteDefinition route) {
        log.info("添加路由: {}", route);
        return dynamicRouteService.add(route)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.CREATED)))
                .onErrorResume(e -> Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @PutMapping
    @Operation(summary = "更新路由", description = "更新单个路由定义")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "500", description = "更新失败")
    public Mono<ResponseEntity<Void>> update(
            @Parameter(description = "路由定义", required = true) 
            @RequestBody RouteDefinition route) {
        log.info("更新路由: {}", route);
        return dynamicRouteService.update(route)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                .onErrorResume(e -> Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @DeleteMapping("/{routeId}")
    @Operation(summary = "删除路由", description = "根据路由ID删除路由定义")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "500", description = "删除失败")
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(description = "路由ID", required = true) 
            @PathVariable String routeId) {
        log.info("删除路由: {}", routeId);
        return dynamicRouteService.delete(routeId)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                .onErrorResume(e -> Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @GetMapping("/{routeId}")
    @Operation(summary = "获取路由", description = "根据路由ID获取路由定义")
    @ApiResponse(
        responseCode = "200", 
        description = "获取成功", 
        content = @Content(schema = @Schema(implementation = RouteDefinition.class))
    )
    @ApiResponse(responseCode = "404", description = "路由不存在")
    public Mono<ResponseEntity<RouteDefinition>> getRoute(
            @Parameter(description = "路由ID", required = true) 
            @PathVariable String routeId) {
        log.info("获取路由: {}", routeId);
        return dynamicRouteService.getRoute(routeId)
                .map(route -> new ResponseEntity<>(route, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    @Operation(summary = "获取所有路由", description = "获取所有路由定义")
    @ApiResponse(
        responseCode = "200", 
        description = "获取成功", 
        content = @Content(schema = @Schema(implementation = RouteDefinition.class))
    )
    public Mono<ResponseEntity<List<RouteDefinition>>> getRoutes() {
        log.info("获取所有路由");
        return dynamicRouteService.getRoutes()
                .collectList()
                .map(routes -> new ResponseEntity<>(routes, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "刷新路由", description = "强制刷新Gateway路由配置")
    @ApiResponse(responseCode = "200", description = "刷新成功")
    @ApiResponse(responseCode = "500", description = "刷新失败")
    public Mono<ResponseEntity<Void>> refreshRoutes() {
        log.info("手动刷新路由");
        return dynamicRouteService.refreshRoutes()
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                .onErrorResume(e -> Mono.just(new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR)));
    }
} 