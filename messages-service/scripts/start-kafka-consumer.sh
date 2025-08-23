#!/bin/bash

# Kafka消费服务快速启动脚本
# 用于快速启动和测试Kafka消费功能

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java未安装，请先安装Java 17或更高版本"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        log_error "Java版本过低，需要Java 17或更高版本，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    log_success "Java版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 检查Maven环境
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    log_success "Maven版本: $(mvn -version | head -n 1)"
}

# 检查Docker环境
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_warning "Docker未安装，将跳过Kafka容器检查"
        return 1
    fi
    
    if ! docker info &> /dev/null; then
        log_warning "Docker服务未启动，将跳过Kafka容器检查"
        return 1
    fi
    
    log_success "Docker环境检查通过"
    return 0
}

# 启动Kafka容器
start_kafka_container() {
    if ! check_docker; then
        return 1
    fi
    
    log_info "检查Kafka容器状态..."
    
    if docker ps | grep -q "kafka"; then
        log_success "Kafka容器已在运行"
        return 0
    fi
    
    log_info "启动Kafka容器..."
    
    # 创建Kafka网络
    docker network create kafka-network 2>/dev/null || true
    
    # 启动Zookeeper
    docker run -d --name zookeeper \
        --network kafka-network \
        -p 2181:2181 \
        -e ZOOKEEPER_CLIENT_PORT=2181 \
        confluentinc/cp-zookeeper:latest
    
    # 等待Zookeeper启动
    log_info "等待Zookeeper启动..."
    sleep 10
    
    # 启动Kafka
    docker run -d --name kafka \
        --network kafka-network \
        -p 9092:9092 \
        -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
        -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
        confluentinc/cp-kafka:latest
    
    # 等待Kafka启动
    log_info "等待Kafka启动..."
    sleep 15
    
    log_success "Kafka容器启动完成"
    return 0
}

# 创建测试主题
create_test_topics() {
    if ! docker ps | grep -q "kafka"; then
        log_warning "Kafka容器未运行，跳过主题创建"
        return 1
    fi
    
    log_info "创建测试主题..."
    
    # 创建测试主题
    docker exec kafka kafka-topics --create \
        --topic test-topic \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1
    
    # 创建死信队列主题
    docker exec kafka kafka-topics --create \
        --topic test-dlq-topic \
        --bootstrap-server localhost:9092 \
        --partitions 1 \
        --replication-factor 1
    
    # 创建重试主题
    docker exec kafka kafka-topics --create \
        --topic test-retry-topic \
        --bootstrap-server localhost:9092 \
        --partitions 1 \
        --replication-factor 1
    
    log_success "测试主题创建完成"
    return 0
}

# 编译项目
build_project() {
    log_info "编译项目..."
    
    if ! mvn clean compile -q; then
        log_error "项目编译失败"
        exit 1
    fi
    
    log_success "项目编译完成"
}

# 运行测试
run_tests() {
    log_info "运行单元测试..."
    
    if ! mvn test -q; then
        log_warning "部分测试失败，但继续执行"
    else
        log_success "单元测试通过"
    fi
}

# 启动应用
start_application() {
    log_info "启动应用..."
    
    # 使用测试配置文件启动
    mvn spring-boot:run -Dspring-boot.run.profiles=integration-test &
    APP_PID=$!
    
    # 等待应用启动
    log_info "等待应用启动..."
    sleep 30
    
    # 检查应用是否启动成功
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "应用启动成功，PID: $APP_PID"
        echo $APP_PID > .app.pid
    else
        log_error "应用启动失败"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi
}

# 测试Kafka消费
test_kafka_consumption() {
    log_info "测试Kafka消费功能..."
    
    # 等待一段时间让消费者启动
    sleep 10
    
    # 检查消费状态
    if curl -s http://localhost:8080/api/auto-consume/status/user-service > /dev/null 2>&1; then
        log_success "Kafka消费服务启动成功"
    else
        log_warning "Kafka消费服务可能未启动，请检查日志"
    fi
}

# 显示使用说明
show_usage() {
    echo -e "${BLUE}Kafka消费服务快速启动脚本${NC}"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示此帮助信息"
    echo "  -k, --kafka    启动Kafka容器"
    echo "  -t, --topics   创建测试主题"
    echo "  -b, --build    编译项目"
    echo "  -r, --run      运行测试"
    echo "  -s, --start    启动应用"
    echo "  -a, --all      执行所有步骤"
    echo ""
    echo "示例:"
    echo "  $0 --all        # 执行所有步骤"
    echo "  $0 --kafka      # 仅启动Kafka容器"
    echo "  $0 --start      # 仅启动应用"
}

# 停止应用
stop_application() {
    if [ -f .app.pid ]; then
        APP_PID=$(cat .app.pid)
        if kill -0 $APP_PID 2>/dev/null; then
            log_info "停止应用 (PID: $APP_PID)..."
            kill $APP_PID
            rm -f .app.pid
            log_success "应用已停止"
        else
            log_warning "应用进程不存在"
            rm -f .app.pid
        fi
    else
        log_warning "未找到应用PID文件"
    fi
}

# 清理资源
cleanup() {
    log_info "清理资源..."
    
    # 停止应用
    stop_application
    
    # 停止Kafka容器
    if docker ps | grep -q "kafka"; then
        log_info "停止Kafka容器..."
        docker stop kafka zookeeper 2>/dev/null || true
        docker rm kafka zookeeper 2>/dev/null || true
        log_success "Kafka容器已清理"
    fi
    
    # 清理网络
    docker network rm kafka-network 2>/dev/null || true
}

# 主函数
main() {
    case "${1:-}" in
        -h|--help)
            show_usage
            exit 0
            ;;
        -k|--kafka)
            start_kafka_container
            ;;
        -t|--topics)
            create_test_topics
            ;;
        -b|--build)
            build_project
            ;;
        -r|--run)
            run_tests
            ;;
        -s|--start)
            start_application
            ;;
        -a|--all)
            check_java
            check_maven
            start_kafka_container
            create_test_topics
            build_project
            run_tests
            start_application
            test_kafka_consumption
            ;;
        --stop)
            stop_application
            ;;
        --cleanup)
            cleanup
            ;;
        *)
            show_usage
            exit 1
            ;;
    esac
}

# 信号处理
trap cleanup EXIT
trap 'log_info "收到中断信号，正在清理..."; cleanup; exit 1' INT TERM

# 执行主函数
main "$@"
