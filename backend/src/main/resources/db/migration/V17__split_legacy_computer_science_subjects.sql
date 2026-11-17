UPDATE documents
SET subject = CASE
    WHEN lower(original_filename) LIKE '%javascript%' OR lower(original_filename) LIKE '%java-script%' THEN 'JavaScript'
    WHEN lower(original_filename) LIKE '%python%' OR lower(original_filename) LIKE '%pyth%' THEN 'Python'
    WHEN lower(original_filename) LIKE '%java%' THEN 'Java'
    WHEN lower(original_filename) LIKE '%data structure%' OR lower(original_filename) LIKE '%algorithm%' THEN 'Data Structures & Algorithms'
    WHEN lower(original_filename) LIKE '%database%' OR lower(original_filename) LIKE '%dbms%' OR lower(original_filename) LIKE '%sql%' THEN 'Database Systems (DBMS)'
    WHEN lower(original_filename) LIKE '%network%' THEN 'Computer Networks'
    WHEN lower(original_filename) LIKE '%operating system%' THEN 'Operating Systems'
    ELSE 'General'
END
WHERE lower(coalesce(subject, '')) IN ('', 'computer science');
