UPDATE bookable_services
SET
    name = 'Hair Consultation',
    description = 'Professional consultation before hair treatment',
    duration_minutes = 30
WHERE id = 1;


UPDATE bookable_services
SET
    name = 'Women Haircut',
    description = 'Classic women haircut with styling',
    duration_minutes = 60
WHERE id = 2;


UPDATE bookable_services
SET
    name = 'Hair Coloring',
    description = 'Full hair coloring service',
    duration_minutes = 180
WHERE id = 3;


INSERT INTO bookable_services
(
    name,
    description,
    duration_minutes,
    active
)
VALUES
    (
        'Men Haircut',
        'Professional haircut for men',
        45,
        true
    ),
    (
        'Kids Haircut',
        'Haircut for children up to 12 years',
        30,
        true
    ),
    (
        'Teen Haircut',
        'Modern haircut for teenagers',
        45,
        true
    ),
    (
        'Hair Wash and Styling',
        'Hair washing and professional styling',
        45,
        true
    ),
    (
        'Balayage Highlights',
        'Highlights and balayage coloring technique',
        240,
        true
    ),
    (
        'Keratin Treatment',
        'Hair smoothing and repair treatment',
        150,
        true
    ),
    (
        'Hair Perm',
        'Professional permanent wave treatment',
        180,
        true
    ),
    (
        'Eyebrow Styling',
        'Eyebrow shaping and styling',
        30,
        true
    ),
    (
        'Eyelash Lifting',
        'Eyelash lifting treatment',
        60,
        true
    ),
    (
        'Professional Makeup',
        'Professional makeup session',
        60,
        true
    );