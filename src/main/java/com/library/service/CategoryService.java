package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.library.common.BizException;
import com.library.common.RedisCache;
import com.library.common.RedisKeys;
import com.library.dto.CategoryRequest;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import com.library.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 图书分类管理。分类列表变动少、读取频繁，放 Redis 缓存 10 分钟，写操作主动清除。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;
    private final RedisCache cache;

    public List<CategoryVO> list() {
        List<CategoryVO> cached = cache.get(RedisKeys.CATEGORY_CACHE, new TypeReference<>() {
        });
        if (cached != null) {
            return cached;
        }
        List<CategoryVO> list = categoryMapper.selectWithBookCount();
        cache.set(RedisKeys.CATEGORY_CACHE, list, RedisKeys.CATEGORY_TTL);
        return list;
    }

    @Transactional
    public CategoryVO create(CategoryRequest request) {
        ensureNameUnique(request.getName(), null);
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setDescription(request.getDescription());
        categoryMapper.insert(category);
        invalidate();
        return toVO(category, 0L);
    }

    @Transactional
    public CategoryVO update(Long id, CategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        ensureNameUnique(request.getName(), id);
        category.setName(request.getName().trim());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setDescription(request.getDescription());
        categoryMapper.updateById(category);
        invalidate();
        Long count = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        return toVO(category, count == null ? 0 : count);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        Long books = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        if (books != null && books > 0) {
            throw BizException.conflict("该分类下还有 " + books + " 种图书，请先移动或删除图书");
        }
        categoryMapper.deleteById(id);
        invalidate();
    }

    private void ensureNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BizException.conflict("分类名称已存在");
        }
    }

    private void invalidate() {
        cache.delete(RedisKeys.CATEGORY_CACHE, RedisKeys.HOME_STATS_CACHE, RedisKeys.ADMIN_STATS_CACHE);
    }

    private CategoryVO toVO(Category category, Long bookCount) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSort(category.getSort());
        vo.setDescription(category.getDescription());
        vo.setBookCount(bookCount);
        return vo;
    }
}
