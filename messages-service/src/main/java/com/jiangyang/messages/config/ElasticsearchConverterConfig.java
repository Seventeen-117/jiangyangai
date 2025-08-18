package com.jiangyang.messages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Elasticsearch 自定义转换器配置
 * 解决 ES 中日期仅为 yyyy-MM-dd 字符串时反序列化到 LocalDateTime 失败的问题
 */
@Configuration
public class ElasticsearchConverterConfig {

	@Bean
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(new StringToLocalDateTimeConverter());
		converters.add(new LongToLocalDateTimeConverter());
		converters.add(new DateToLocalDateTimeConverter());
		converters.add(new StringToDateConverter());
		converters.add(new LongToDateConverter());
		return new ElasticsearchCustomConversions(converters);
	}

	@ReadingConverter
	static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		@Override
		public LocalDateTime convert(String source) {
			if (source == null || source.isEmpty()) {
				return null;
			}
			String text = source.trim();
			try {
				// 带时区/UTC格式
				if (text.endsWith("Z") || text.contains("+")) {
					return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
				}
			} catch (Exception ignored) {}
			try {
				// 完整本地日期时间，如 2025-08-17T12:34:56 或 2025-08-17 12:34:56
				String normalized = text.replace(' ', 'T');
				return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			} catch (Exception ignored) {}
			try {
				// 仅日期，如 2025-08-17 -> 默认补全为当天 00:00:00
				LocalDate date = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
				return date.atStartOfDay();
			} catch (Exception ignored) {}
			try {
				// 纯数字时间戳（毫秒）
				long epochMillis = Long.parseLong(text);
				return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
			} catch (Exception ignored) {}
			// 无法解析则返回 null，由上层处理
			return null;
		}
	}

	@ReadingConverter
	static class LongToLocalDateTimeConverter implements Converter<Long, LocalDateTime> {
		@Override
		public LocalDateTime convert(Long source) {
			if (source == null) {
				return null;
			}
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(source), ZoneId.systemDefault());
		}
	}

	@ReadingConverter
	static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
		@Override
		public LocalDateTime convert(Date source) {
			if (source == null) {
				return null;
			}
			return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		}
	}

	@ReadingConverter
	static class StringToDateConverter implements Converter<String, Date> {
		@Override
		public Date convert(String source) {
			if (source == null || source.isEmpty()) {
				return null;
			}
			String text = source.trim();
			try {
				if (text.endsWith("Z") || text.contains("+")) {
					Instant instant = Instant.parse(text);
					return Date.from(instant);
				}
			} catch (Exception ignored) {}
			try {
				String normalized = text.replace(' ', 'T');
				LocalDateTime ldt = LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			} catch (Exception ignored) {}
			try {
				LocalDate ld = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
				return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
			} catch (Exception ignored) {}
			try {
				long epochMillis = Long.parseLong(text);
				return new Date(epochMillis);
			} catch (Exception ignored) {}
			return null;
		}
	}

	@ReadingConverter
	static class LongToDateConverter implements Converter<Long, Date> {
		@Override
		public Date convert(Long source) {
			if (source == null) {
				return null;
			}
			return new Date(source);
		}
	}
}


