# ADR 0001: Booking domain rules and persistence contract

- **Status:** Accepted
- **Date:** 2026-07-30

## Context

A booking has a lifecycle represented by `BookingStatus`.

Previously, parts of the application could change a booking status through the repository method:

```java
updateStatus(Long bookingId, BookingStatus status)
```

This allowed application or infrastructure code to persist a new status without requiring the corresponding domain operation on the `Booking` aggregate.

The system also needs to handle concurrent attempts to change the same booking without silently overwriting a previously committed change.

## Decision

### Domain ownership of state transitions

The `Booking` aggregate owns all business rules related to its state.

Status changes must be performed through domain methods:

```java
booking.confirm();
booking.cancel();
```

Deletion eligibility must be checked through:

```java
booking.ensureCanBeDeleted();
```

Direct status assignment outside the domain model is not part of the application contract.

### Allowed status transitions

The following transitions are allowed:

- `PENDING` → `CONFIRMED`
- `PENDING` → `CANCELLED`
- `CONFIRMED` → `CANCELLED`

Other transitions are rejected by the domain model.

A booking may be physically deleted only when its status is `CANCELLED`.

### Authorization boundary

Authorization determines whether the authenticated user is allowed to attempt an operation.

- A regular user may work only with their own bookings.
- An administrator may work with any booking.
- A regular user may not delete bookings.
- Unauthorized access to another user's booking is represented as not found.

Authorization does not replace domain validation. Even an authorized administrator cannot perform a state transition forbidden by the `Booking` aggregate.

### Persistence contract

The repository persists the resulting aggregate state but does not decide whether a business operation is allowed.

The status-specific repository method was replaced with:

```java
Optional<Booking> update(Booking booking);
```

The application flow is:

```java
Booking booking = bookingRepository.findById(id)
        .orElseThrow();

booking.cancel();

bookingRepository.update(booking);
```

The repository implementation may update only fields that the domain model allows to change.

At present, `Booking` allows only its `status` to change after creation. The user, service, start time, and end time remain unchanged.

### Creation and restoration

New bookings must be created through:

```java
Booking.create(...);
```

Existing bookings loaded from persistence must be reconstructed through:

```java
Booking.restore(...);
```

`Booking.create(...)` creates a new booking without an identifier and with the initial status `PENDING`.

`Booking.restore(...)` requires an existing identifier and restores persisted state.

### Concurrent updates

`BookingEntity` uses optimistic locking through a JPA `@Version` field.

Concurrent operations based on the same persisted version must not silently overwrite each other. One operation may succeed, while the conflicting operation must fail.

## Consequences

### Positive

- Business rules are centralized in the `Booking` aggregate.
- Application and infrastructure code cannot use the repository contract to create arbitrary status transitions.
- JPA and in-memory repository implementations follow the same update semantics.
- Concurrent updates cannot silently cause lost updates.
- Creation, restoration, domain modification, and persistence have clearly separated responsibilities.

### Negative

- Updating a booking requires loading the aggregate before changing its state.
- New mutable booking fields require corresponding domain methods and persistence mapping changes.
- Repository implementations and tests must remain consistent with the domain contract.

## Alternatives considered

### Keep `updateStatus(id, status)`

Rejected because it exposes a persistence operation that can bypass domain state-transition rules.

### Allow public setters on `Booking`

Rejected because callers could create invalid state transitions without using domain behavior.

### Use database updates without version checking

Rejected because concurrent operations could overwrite each other without detection.