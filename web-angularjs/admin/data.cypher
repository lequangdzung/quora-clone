begin
CREATE (userdung: USER {name: 'dung', password: 'ngocminh', email: 'dung@gmail.com'})
CREATE (userdungdeputy: USER {name: 'dung-deputy', password: 'ngocminh', email: 'dungdeputy@gmail.com'})
CREATE (userdungmembe:USER {name: 'dung-member', password: 'ngocminh', email: 'dungmember@gmail.com'})

CREATE (userbang: USER {name: 'bang', password: 'ngocminh', email: 'bang@gmail.com'})
CREATE (userbangdeputy: USER {name: 'bang-deputy', password: 'ngocminh', email: 'bangdeputy@gmail.com'})
CREATE (userbangmember:USER {name: 'bang-member', password: 'ngocminh', email: 'bangmember@gmail.com'})

CREATE (useran: USER {name: 'an', password: 'ngocminh', email: 'an@gmail.com'})
CREATE (userandeputy: USER {name: 'an-deputy', password: 'ngocminh', email: 'andeputy@gmail.com'})
CREATE (useranmember:USER {name: 'an-member', password: 'ngocminh', email: 'anmember@gmail.com'})

CREATE (groupa: GROUP {name: 'group a'})
CREATE (groupb: GROUP {name: 'group b'})
CREATE (groupc: GROUP {name: 'group c'})
CREATE (groupd: GROUP {name: 'group d'})

CREATE (typetest: CONTENT_TYPE {name: 'test'})

CREATE (testproc: PROCEDURE {name: 'test', lab: 'TEST_PROCEDURE'})

CREATE typetest-[:PROCEDURE]->testproc

CREATE (step1: STEP {name: 'step1', description: 'input data'})
CREATE (step2: STEP {name: 'step2', description: 'group a approval'})
CREATE (step3: STEP {name: 'step3', description: 'group b input data'})
CREATE (step4: STEP {name: 'step4', description: 'group b approve'})
CREATE (step5a: STEP {name: 'step5a', description: 'group c input data'})
CREATE (step6a: STEP {name: 'step6a', description: 'finish branch a'})

CREATE (step5b: STEP {name: 'step5b', description: 'group d input data'})
CREATE (step6b: STEP {name: 'step6b', description: 'finish branch b'})

CREATE testproc-[:HAS]->step1
CREATE testproc-[:HAS]->step2
CREATE testproc-[:HAS]->step3
CREATE testproc-[:HAS]->step4
CREATE testproc-[:HAS]->step5a
CREATE testproc-[:HAS]->step5b
CREATE testproc-[:HAS]->step6a
CREATE testproc-[:HAS]->step6b

CREATE step1-[:NEXT]->step2
CREATE step2-[:BACK]->step1
CREATE step2-[:NEXT]->step3
CREATE step3-[:BACK]->step2
CREATE step3-[:NEXT]->step4
CREATE step4-[:BACK]->step3

CREATE step4-[:NEXT]->step5a
CREATE step5a-[:NEXT]->step6a
CREATE step6a-[:BACK]->step5a
;
commit