package com.example.springclaudeturtorial.phase4.transaction;

import com.example.springclaudeturtorial.phase4.jpa.domain.Item;
import com.example.springclaudeturtorial.phase4.jpa.repository.ItemRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * TOPIC: Transaction Management — Propagation & Isolation
 *
 * @Transactional là AOP Proxy → 2 gotcha cần nhớ:
 *   1. Self-invocation: this.method() không qua proxy → transaction không apply
 *   2. Private method: proxy không override → transaction không apply
 */
@Service
public class TransactionDemo {

    private final ItemRepository itemRepository;
    private final TransactionDemo self; // inject self để tránh self-invocation

    public TransactionDemo(ItemRepository itemRepository,
                           @Lazy TransactionDemo self) {
        this.itemRepository = itemRepository;
        this.self           = self;
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROPAGATION — hành vi khi method transactional gọi method transactional khác
    // ════════════════════════════════════════════════════════════════════════

    /**
     * REQUIRED (default): tham gia transaction hiện có hoặc tạo mới.
     * Trường hợp phổ biến nhất.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void requiredExample(Long itemId) {
        System.out.println("  [REQUIRED] txn active: " + isTransactionActive());
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setStock(item.getStock() + 1);
        itemRepository.save(item);
        // Nếu method gọi method khác có REQUIRED → dùng CHUNG transaction này
    }

    /**
     * REQUIRES_NEW: luôn tạo transaction MỚI, suspend transaction hiện có.
     * Dùng cho: audit log — cần commit độc lập dù outer transaction rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditLog(String action, Long itemId) {
        System.out.println("  [REQUIRES_NEW] txn active: " + isTransactionActive()
            + " | action=" + action + ", itemId=" + itemId);
        // Commit audit log ngay cả khi outer transaction rollback
    }

    /**
     * NESTED: transaction con trong transaction cha.
     * Savepoint: có thể rollback về điểm savepoint mà không ảnh hưởng outer.
     * Chỉ hoạt động với JDBC (không phải JTA).
     */
    @Transactional(propagation = Propagation.NESTED)
    public void nestedOperation(Long itemId, int delta) {
        System.out.println("  [NESTED] txn active: " + isTransactionActive());
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setStock(item.getStock() + delta);
        itemRepository.save(item);
    }

    /**
     * SUPPORTS: chạy trong transaction nếu có, không thì chạy không transaction.
     * Dùng cho: read operation linh hoạt.
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Item readItem(Long itemId) {
        System.out.println("  [SUPPORTS] txn active: " + isTransactionActive());
        return itemRepository.findById(itemId).orElseThrow();
    }

    /**
     * NOT_SUPPORTED: luôn chạy KHÔNG có transaction (suspend nếu có).
     * Dùng cho: bulk read không cần transaction để tránh lock.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public long countItems() {
        System.out.println("  [NOT_SUPPORTED] txn active: " + isTransactionActive());
        return itemRepository.count();
    }

    /**
     * NEVER: throw exception nếu đang có transaction.
     * Dùng để đảm bảo operation KHÔNG chạy trong transaction.
     */
    @Transactional(propagation = Propagation.NEVER)
    public void operationThatMustNotBeInTransaction() {
        System.out.println("  [NEVER] txn active: " + isTransactionActive());
        // Nếu gọi trong transaction → TransactionDefinition.NEVER throws IllegalTransactionStateException
    }


    // ════════════════════════════════════════════════════════════════════════
    // ISOLATION LEVELS — mức độ cô lập giữa các transaction song song
    // ════════════════════════════════════════════════════════════════════════

    /**
     * READ_UNCOMMITTED: đọc được data chưa commit (dirty read).
     * Nguy hiểm nhất — hầu như không dùng.
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Item readUncommitted(Long id) {
        return itemRepository.findById(id).orElseThrow();
    }

    /**
     * READ_COMMITTED (default của PostgreSQL, SQL Server):
     * Chỉ đọc data đã commit. Tránh dirty read.
     * Vẫn có: non-repeatable read, phantom read.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Item readCommitted(Long id) {
        return itemRepository.findById(id).orElseThrow();
    }

    /**
     * REPEATABLE_READ (default của MySQL):
     * Đảm bảo đọc cùng row 2 lần → cùng kết quả.
     * Tránh: dirty read, non-repeatable read.
     * Vẫn có: phantom read.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Item repeatableRead(Long id) {
        Item first  = itemRepository.findById(id).orElseThrow();
        // ... làm gì đó ...
        Item second = itemRepository.findById(id).orElseThrow(); // guaranteed same value
        return second;
    }

    /**
     * SERIALIZABLE: isolation cao nhất.
     * Tránh tất cả read phenomena, nhưng chậm nhất (table lock).
     * Chỉ dùng cho financial operations.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void criticalFinancialOperation(Long id) {
        // Mọi transaction khác phải chờ khi operation này đang chạy
        Item item = itemRepository.findById(id).orElseThrow();
        System.out.println("  [SERIALIZABLE] processing: " + item.getName());
    }


    // ════════════════════════════════════════════════════════════════════════
    // ROLLBACK RULES — kiểm soát khi nào rollback
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Default: rollback chỉ với RuntimeException và Error.
     * CheckedException KHÔNG rollback theo mặc định!
     */
    @Transactional
    public void defaultRollbackBehavior(Long id, boolean throwChecked) throws Exception {
        Item item = itemRepository.findById(id).orElseThrow();
        item.setStock(item.getStock() + 100);
        itemRepository.save(item);

        if (throwChecked) {
            throw new Exception("Checked exception — KHÔNG rollback theo mặc định!");
            // → stock vẫn tăng 100 dù throw exception
        }
    }

    /**
     * rollbackFor: rollback với checked exception cụ thể.
     * noRollbackFor: KHÔNG rollback với runtime exception cụ thể.
     */
    @Transactional(
        rollbackFor    = { Exception.class },       // rollback với mọi exception
        noRollbackFor  = { IllegalArgumentException.class } // trừ cái này
    )
    public void customRollbackRules(Long id) {
        Item item = itemRepository.findById(id).orElseThrow();
        item.setStock(-999); // invalid
        itemRepository.save(item);
        // Với rollbackFor=Exception.class, mọi exception đều rollback
    }


    // ════════════════════════════════════════════════════════════════════════
    // GOTCHA 1: Self-invocation — @Transactional không apply
    // ════════════════════════════════════════════════════════════════════════

    public void outerMethod_BAD(Long id) {
        System.out.println("  [GOTCHA] outerMethod_BAD: gọi this.innerMethod()");
        this.innerTransactionalMethod(id);  // BYPASS proxy → @Transactional KHÔNG apply!
    }

    public void outerMethod_GOOD(Long id) {
        System.out.println("  [GOTCHA] outerMethod_GOOD: gọi self.innerMethod()");
        self.innerTransactionalMethod(id);  // qua proxy → @Transactional apply!
    }

    @Transactional
    public void innerTransactionalMethod(Long id) {
        System.out.println("  [inner] txn active: " + isTransactionActive());
        itemRepository.findById(id);
    }


    // ════════════════════════════════════════════════════════════════════════
    // OPTIMISTIC LOCKING (@Version)
    // ════════════════════════════════════════════════════════════════════════

    @Transactional
    public void demonstrateOptimisticLocking(Long itemId) {
        // Simulate: 2 users load cùng item (same version)
        Item item1 = itemRepository.findById(itemId).orElseThrow();
        Item item2 = itemRepository.findById(itemId).orElseThrow();

        System.out.println("  item1 version=" + item1.getVersion());
        System.out.println("  item2 version=" + item2.getVersion());

        // User 1 update thành công → version tăng lên 1
        item1.setStock(item1.getStock() + 10);
        itemRepository.saveAndFlush(item1);
        System.out.println("  After item1 save, version=" + item1.getVersion());

        // User 2 cố update với version cũ → OptimisticLockException
        try {
            item2.setStock(item2.getStock() + 20);
            itemRepository.saveAndFlush(item2); // sẽ throw!
        } catch (Exception e) {
            System.out.println("  item2 save FAILED (optimistic lock): "
                + e.getClass().getSimpleName());
        }
    }


    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
