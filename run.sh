#!/bin/bash

function test {
	for i in 1 2 3 4 5
	do
		eval $1
	done
} 

mkdir wyniki
mkdir wyniki/stm
mkdir wyniki/single
mkdir wyniki/porownanie

function compare {
    for factor in 0 1 2 4 6 8 10
	do
	    for i in 1 2 3 4 5 6 7 8 9 10
            do
		eval $1
	    done
	done
}

compare "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dop.factor="\$factor" > wyniki/porownanie/stm_\$factor\$i"
#compare "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" -Dop.factor="\$factor" > wyniki/porownanie/single_\$factor\$i"


#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.SingleThreadPublishingApp" > wyniki/single/\$i"

#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="6" > wyniki/stm/6\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="5" > wyniki/stm/5\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="3" > wyniki/stm/3\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="1" > wyniki/stm/1\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="2" > wyniki/stm/2\$i"
#test "mvn exec:exec -Dmain.class="com.marekmaj.hfplatform.StmPublishingApp" -Dstm.threads="4" > wyniki/stm/4\$i"
