package com.library.catalog.infrastructure.persistence.jpa;

import com.library.catalog.application.query.BookSearchCriteria;
import com.library.catalog.domain.model.Book;
import com.library.catalog.domain.model.BookAuthor;
import com.library.catalog.domain.repository.CustomBookRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class BookRepositoryImpl implements CustomBookRepository {

    private final EntityManager entityManager;

    public BookRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Book> search(BookSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> root = query.from(Book.class);

        List<Predicate> predicates = buildPredicates(criteria, cb, root);
        query.where(predicates.toArray(new Predicate[0]));

        List<Order> orders = buildOrders(pageable, cb, root);
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }

        TypedQuery<Book> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Book> results = typedQuery.getResultList();
        long total = countResults(criteria, cb);

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(BookSearchCriteria criteria, CriteriaBuilder cb, Root<Book> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.title() != null && !criteria.title().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("title")),
                "%" + criteria.title().toLowerCase() + "%"));
        }

        if (criteria.status() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.status()));
        }

        if (criteria.publisherId() != null && !criteria.publisherId().isBlank()) {
            predicates.add(cb.equal(root.get("publisherId"), criteria.publisherId()));
        }

        if (criteria.language() != null && !criteria.language().isBlank()) {
            predicates.add(cb.equal(root.get("language"), criteria.language()));
        }

        if (criteria.authorName() != null && !criteria.authorName().isBlank()) {
            Join<Book, BookAuthor> authorJoin = root.join("authors", JoinType.INNER);
            predicates.add(cb.like(cb.lower(authorJoin.get("authorName")),
                "%" + criteria.authorName().toLowerCase() + "%"));
        }

        if (criteria.categoryId() != null && !criteria.categoryId().isBlank()) {
            predicates.add(cb.isMember(criteria.categoryId(), root.get("categoryIds")));
        }

        return predicates;
    }

    private List<Order> buildOrders(Pageable pageable, CriteriaBuilder cb, Root<Book> root) {
        List<Order> orders = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            for (Sort.Order sort : pageable.getSort()) {
                Path<Object> path = root.get(sort.getProperty());
                orders.add(sort.isAscending() ? cb.asc(path) : cb.desc(path));
            }
        }
        return orders;
    }

    private long countResults(BookSearchCriteria criteria, CriteriaBuilder cb) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Book> countRoot = countQuery.from(Book.class);

        List<Predicate> predicates = buildPredicates(criteria, cb, countRoot);
        countQuery.where(predicates.toArray(new Predicate[0]));
        countQuery.select(cb.count(countRoot));

        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
