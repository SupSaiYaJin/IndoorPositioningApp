package com.saiya.indoorposapp.tools.postioning;

import java.util.List;

/**
 * 计算上传指纹值的滤波器
 */
public interface Filter {

    float doFilter(List<Float> values);
}
