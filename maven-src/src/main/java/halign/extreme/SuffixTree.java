package halign.extreme;

import java.util.*;
/**
 * <p>
 * 后缀树的实现：Ukkonen构造法，
 * 论文翻译文章：<code>http://www.oschina.net/translate/ukkonens-suffix-tree-algorithm-in-plain-english</code>
 * 论文中使用边来保存字符信息，此处实现使用节点保存字符信息，没有差别。</b>
 * 同时树的结构通过子节点和兄弟节点保存，即遍历一个节点的子节点，需先找到其直接子节点，然后通过该子节点访问其兄弟节点找到全部子节点
 */
public class SuffixTree {
	private  Node root = new Node(new char[0]);// 根节点，根节点无chars，所以new char[0]
	// active point，一个三元组：(active_node,active_edge,active_length)
	// active_node是当前的活动点，用节点代表，active_edge是活动的边，这里用节点来表示，active_length是活动的长度
	private ActivePoint activePoint = new ActivePoint(root, null, 0);
	private int reminder = 0;// remainder，表示还剩多少后缀需要插入

	public int minMatchLen = 15;

	/**
	 * 构建后缀树
	 *
	 * @param word
	 */
	public void build(String word) {
		int index = 0;
		char[] chars = word.toCharArray();
		while (index < chars.length) {// 循环建立后缀
			int currenctIndex = index++;// 保存当前的位置,注意：int a=b++;相当于int a=b;b++;不是a=b+1!
			char w = chars[currenctIndex];// 当前的后缀字符

			// this.print();
			//	System.out.println("插之前活动三元组"+activePoint.toString());
			//	System.out.println("插之前reminder：" + String.valueOf(reminder));

			//		if(currenctIndex%1000==1)
			//	   System.out.println("building the prefix center: " + currenctIndex + "  /  "+(chars.length-1));



			if (find(w)) {// 查找是否存在保存有当前后缀字符的节点
				reminder++;// 存在，则将reminder+1，activePoint.length+1，然后返回即可
				continue;
			}

			// 不存在的话，如果reminder==0表示之前在该字符之前未剩余有其他带插入的后缀字符，所以直接插入该后缀字符即可

			if (reminder == 0) {
				// 直接在当前活动节点插入一个节点即可
				// 这里插入的节点包含的字符是从当前字符开始该字符串剩余的全部字符，这里是一个优化，
				// 优化参考自：http://blog.csdn.net/v_july_v/article/details/6897097 (3.6、归纳, 反思, 优化)
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex);
				node.label=currenctIndex;
				// 如果当前活动点无子节点，则将新建的节点作为其子节点即可，否则循环遍历子节点(通过兄弟节点进行保存)
				Node child = activePoint.point.child;
				if (null == child) {
					activePoint.point.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father=activePoint.point;
				/*********
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex);
				 fath = fath.father;
				 }
				 *********/
			} else if(activePoint.index==null){

				//正好前一条边存了reminder的那些，插入的是一个新字符，直接建立个新边存该字符，并把之前欠下的补充
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex-reminder);
				node.label=currenctIndex-reminder;


				// 如果当前活动点无子节点，则将新建的节点作为其子节点即可，否则循环遍历子节点(通过兄弟节点进行保存)
				Node child = activePoint.point.child;
				if (null == child) {
					activePoint.point.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.point;
				/************
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 *************/



				// 分割完成之后需根据规则1和规则3进行区分对待
				// 按照规则1进行处理
				if (root == activePoint.point) {// 活动节点是根节点的情况
					// activePoint.point == root
					// 按照规则3进行处理
				} else if (null == activePoint.point.suffixNode) {// 无后缀节点，则活动节点变为root
					activePoint.point = root;
				} else {// 否则活动节点变为当前活动节点的后缀节点
					activePoint.point = activePoint.point.suffixNode;
				}
				// 活动边和活动边长度都重置
				activePoint.index = null;
				activePoint.length = 0;
				// 递归处理剩余的待插入后缀
				innerSplit(chars, currenctIndex, activePoint.point);
			}	else if(reminder-getNodeString(activePoint.point).length()<activePoint.index.chars.length){//这里不能是reminder，而是reminder-活动点到跟的距离
				// 如果reminder>0，则说明该字符之前存在剩余字符，需要进行分割，然后插入新的后缀字符
				Node splitNode = activePoint.index;// 待分割的节点即为活动边(active_edge)
				// 创建切分后的节点，放到当前节点的子节点
				// 该节点继承了当前节点的子节点以及后缀节点信息
				//新建一个node当作index的儿子，index变成内部节点
				Node node = new Node(Arrays.copyOfRange(splitNode.chars, activePoint.length, splitNode.chars.length));// 从活动边长度开始截取剩余字符作为子节点
				node.child = splitNode.child;

				Node child = splitNode.child;
				while(null!=child){
					child.father=node;
					child=child.brother;
				}


				node.suffixNode = splitNode.suffixNode;
				splitNode.child = node;
				node.father = splitNode;
				splitNode.suffixNode = null;
				//node.position = (ArrayList<Integer>) splitNode.position.clone();
				if(splitNode.chars[splitNode.chars.length-1]=='$')
					node.label=splitNode.label;
				// 创建新插入的节点，放到当前节点的子节点(通过子节点的兄弟节点保存)
				Node newNode = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));// 插入新的后缀字符
				splitNode.child.brother = newNode;
				newNode.father=splitNode;
				splitNode.chars = Arrays.copyOfRange(splitNode.chars, 0, activePoint.length);// 修改当前节点的字符
				//newNode.position.add(currenctIndex-reminder);
				newNode.label=currenctIndex-reminder;

				//Node fath = newNode.father;
				/***************
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 ****************/

				// 分割完成之后需根据规则1和规则3进行区分对待
				// 按照规则1进行处理
				if (root == activePoint.point) {// 活动节点是根节点的情况
					// activePoint.point == root
					// 按照规则3进行处理
				} else if (null == activePoint.point.suffixNode) {// 无后缀节点，则活动节点变为root
					activePoint.point = root;
				} else {// 否则活动节点变为当前活动节点的后缀节点
					activePoint.point = activePoint.point.suffixNode;
				}
				// 活动边和活动边长度都重置
				activePoint.index = null;
				activePoint.length = 0;
				// 递归处理剩余的待插入后缀
				innerSplit(chars, currenctIndex, splitNode);
			}
			else if(reminder-getNodeString(activePoint.point).length()==activePoint.index.chars.length){
				//直接在活动边的结点上插入一个新的儿子，不用插入内部节点了
				Node node = new Node(Arrays.copyOfRange(chars, currenctIndex, chars.length));
				//node.position.add(currenctIndex-reminder);
				node.label=currenctIndex-reminder;

				// 如果当前活动点无子节点，则将新建的节点作为其子节点即可，否则循环遍历子节点(通过兄弟节点进行保存)
				Node child = activePoint.index.child;
				if (null == child) {
					activePoint.index.child = node;
				} else {
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.index;
				/***********
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(currenctIndex-reminder);
				 fath = fath.father;
				 }
				 ************/

				Node ttmp=activePoint.index;
				if (root == activePoint.point) {// 活动节点是根节点的情况
					// activePoint.point == root
					// 按照规则3进行处理
				} else if (null == activePoint.point.suffixNode) {// 无后缀节点，则活动节点变为root
					activePoint.point = root;
				} else {// 否则活动节点变为当前活动节点的后缀节点
					activePoint.point = activePoint.point.suffixNode;
				}
				// 活动边和活动边长度都重置
				activePoint.index = null;
				activePoint.length = 0;
				// 递归处理剩余的待插入后缀
				innerSplit(chars, currenctIndex, ttmp);
			}
			//***********
			if(index==chars.length&&reminder > 0){
				System.out.println("此处不应该出现！");
				index-=reminder;
				reminder=0;
				activePoint.point = root;
				activePoint.index = null;
				activePoint.length=0;
			}
			//***********
		}

	}

	/**
	 * 处理剩余的待插入后缀
	 * @param chars	构建后缀树的全部字符
	 * @param currenctIndex	当前已处理到的字符位置
	 * @param prefixNode 前继节点，即已经进行分割的节点，用于标识后缀节点
	 */
	private void innerSplit(char[] chars, int currenctIndex, Node prefixNode) {
		// 此处计算剩余待插入的后缀的开始位置，例如我们需要插入三个后缀(abx,bx,x)，已处理了abx，则还剩余bx和x，则下面计算的位置就是b的位置
		int start = currenctIndex - reminder + 1;

		//if(null!=root.child&&null!=root.child.suffixNode)
		//	  System.out.println("活动后缀是"+root.child.suffixNode);
		//	System.out.println("当前插入后缀：" + String.copyValueOf(chars, start, currenctIndex - start + 1) + "========");
		//	System.out.println("活动三元组"+activePoint.toString());
		//	System.out.println("reminder：" + String.valueOf(reminder));
		// dealStart表示本次插入我们需要进行查找的开始字符位置，因为由于规则2，可能出现通过后缀节点直接找到活动节点的情况
		// 如通过ab节点的后缀节点，直接找到节点b，那么此时的activePoint(node[b], null, 0)，我们需要从node[b]开始查找x，dealStart的位置就是x的位置

		//这里错了，不应该是activePoint.point.chars.length，而是活动点到根的所有字符个数！！！
		//int dealStart = start + activePoint.point.chars.length + activePoint.length;
		//************后面要修改，每个节点到根的字符个数应该存储*****
		int tmpp=0;
		Node fathh= activePoint.point;
		while(fathh!=root){
			tmpp+=fathh.chars.length;
			fathh=fathh.father;
		}

		int dealStart = start + tmpp + activePoint.length;
		//*********************************************************
		// 从dealStart开始查找所有后缀字符是否都存在(相对与活动点)


//System.out.println("注意了~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		for (int index = dealStart; index <= currenctIndex; index++) {
			char w = chars[index];
			//		System.out.println(index);
			//		System.out.println(currenctIndex);
			//		System.out.print(w);

			if (find(w)) {// 存在，则查找下一个，activePoint.length+1，这里不增加reminder
				//		System.out.println("匹配上了");
				continue;
			}
			//	System.out.println("没匹配上：index："+index);
			Node splitNode = null;// 被分割的节点

			if(null==activePoint.index){// 如果activePoint.index==null，说明没有找到活动边，那么只需要在活动节点下插入一个节点即可
				splitNode=activePoint.point;//******我自己加的zouquan*******

				Node node = new Node(Arrays.copyOfRange(chars, index, chars.length));

				//node.position.add(start);
				node.label=start;
				Node child = activePoint.point.child;
				if(null==child){
					activePoint.point.child = node;
				}else{
					while (null != child.brother) {
						child = child.brother;
					}
					child.brother = node;
				}
				node.father = activePoint.point;
				//修改活动点！！
				//*******************************（我自己加的）
				// 分割完成之后需根据规则1和规则3进行区分对待
				// 按照规则1进行处理
				if (root == activePoint.point) {// 活动节点是根节点的情况
					// activePoint.point == root
					// 按照规则3进行处理
				} else if (null == activePoint.point.suffixNode) {// 无后缀节点，则活动节点变为root
					activePoint.point = root;
				} else {// 否则活动节点变为当前活动节点的后缀节点
					activePoint.point = activePoint.point.suffixNode;
				}
				// 活动边和活动边长度都重置
				activePoint.index = null;
				activePoint.length = 0;
				//*******************************





				/**************
				 Node fath = node.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(start);
				 fath = fath.father;
				 }
				 ***************/
			}else{

				// 开始分割，分割部分同上面的分割
				splitNode = activePoint.index;//(活动边的儿子节点，不是父亲节点)
				// 创建切分后的节点，放到当前节点的子节点
				// 该节点继承了当前节点的子节点以及后缀节点信息
				Node node = new Node(Arrays.copyOfRange(splitNode.chars, activePoint.length, splitNode.chars.length));
				node.child = splitNode.child;

				Node child = splitNode.child;
				while(null!=child){
					child.father=node;
					child=child.brother;
				}

				node.suffixNode = splitNode.suffixNode;
				splitNode.child = node;
				node.father = splitNode;
				splitNode.suffixNode = null;
				//node.position = (ArrayList<Integer>) splitNode.position.clone();
				if(splitNode.chars[splitNode.chars.length-1]=='$')
					node.label=splitNode.label;
				// 创建新插入的节点，放到当前节点的子节点(通过子节点的兄弟节点保存)
				Node newNode = new Node(Arrays.copyOfRange(chars, index, chars.length));
				splitNode.child.brother = newNode;
				newNode.father = splitNode;
				// 修改当前节点的字符数
				splitNode.chars = Arrays.copyOfRange(splitNode.chars, 0, activePoint.length);
				// 规则2，连接后缀节点

				prefixNode.child.suffixNode = splitNode;//注意:前面原代码写的傻逼，明明应该新建一个splitNode,这样predixNode.suufixNode就应该是splitNode.可以作者傻逼，非要把原节点当成splitNode,新建一个节点node存放原来的节点(newNode是存放插入的新边的叶子)，这样prefixNode就变成了splitNode！

				//****计算splitNode到root的边上的字符串长度
				int k=0;
				Node tmp = splitNode;
				while(tmp!=root){
					k+=tmp.chars.length;
					tmp=tmp.father;
				}
				//***************************************
				//newNode.position.add(index-k);
				newNode.label=index-k;
				/******************
				 if(index-k<0){
				 System.out.println("+++++++++++++++++++++++++++");
				 System.out.println(index);
				 System.out.println(k);
				 }

				 /*****************
				 Node fath = newNode.father;
				 while(null!=fath && fath!=root){
				 fath.position.add(index-k);
				 fath = fath.father;
				 }
				 *****************/
			}

			reminder--;

			// 按照规则1进行处理
			if (root == activePoint.point) {// 活动节点是根节点的情况
				// activePoint.point == root

				// 按照规则3进行处理
			} else if (null == activePoint.point.suffixNode) {// 无后缀节点，则活动节点变为root
				activePoint.point = root;
			} else {
				activePoint.point = activePoint.point.suffixNode;
			}

			activePoint.index = null;
			activePoint.length = 0;
			if(reminder > 0){// 如果reminder==0则不需要继续递归插入后缀

				innerSplit(chars, currenctIndex, splitNode);
			}
		}

	}

	/**
	 * 寻找当前活动点的子节点中是否存在包含后缀字符的节点(边)
	 *
	 * @param w
	 * @return
	 */
	private boolean find(char w) {
		final Node start = activePoint.point;
		final Node current = activePoint.index;
		boolean exist = false;
//		System.out.println("find开始"+activePoint.toString());
		if (null == current) {// current==null 无活动边，则从活动点的子节点开始查找
			// 寻找子节点
			Node child = start.child;
			while (null != child) {
				if (child.chars[0] == w) {// 存在
					exist = true;
					if(child.chars.length>1){
						activePoint.index = child;
						activePoint.length++;// activePoint.length++
					}
					else if(child.chars.length==1){ //如果匹配边的字符串长度为1，活动点继续向下移动
						activePoint.point=child;
						activePoint.index = null;
						activePoint.length = 0;
					}
					break;
				} else {
					child = child.brother;
				}
			}
		}

		else if (current.chars.length>activePoint.length&&current.chars[activePoint.length] == w) {// 有活动边，则在活动边上查找
			activePoint.length++;
			exist = true;
			if (current.chars.length == activePoint.length) {
				// 如果活动边的长度已达到活动边的最后一个字符，则将活动点置为活动边，同时活动边置为null，长度置为0
				activePoint.point = current;
				activePoint.index = null;
				activePoint.length = 0;

			}

		}
		else {
			exist = false;
		}
//		System.out.println("find结束"+activePoint.toString());
		return exist;
	}

	/**
	 * 查找给定字符串是否是其子串
	 *
	 * @param word
	 * @return
	 */
	public boolean select(String word) {
		char[] chars = word.toCharArray();
		int index = 0;// 查找到的节点的匹配的位置
		// 查找从根节点开始，遍历子节点
		Node start = root;
		for (int i = 0; i < chars.length; i++) {
			if (start.chars.length < index + 1) {// 如果当前节点已匹配完，则从子节点开始，同时需重置index==0
				index = 0;
				start = start.child;
				while (null != start) {
					// 比较当前节点指定位置(index)的字符是否与待查找字符一致
					// 由于是遍历子节点，所以如果不匹配换个子节点继续
					if (start.chars[index] == chars[i]) {
						index++;
						break;
					} else {
						start = start.brother;
					}
				}
				if (null == start) {// 子节点遍历完都无匹配则返回false
					return false;
				}
			} else if (start.chars[index] == chars[i]) {
				// 如果当前查找到的节点的还有可比较字符，则进行比较，如果不同则直接返回false
				index++;
			} else {
				return false;
			}
		}
		return true;
	}
	/**
	 * 返回node节点的所有后代叶节点的label(position)
	 * @param node
	 * @return
	 */

	public static ArrayList <Integer> getNodeAllLeafSonLabel(Node node){
		ArrayList <Integer> result = new ArrayList<>();

		if(node.chars[node.chars.length-1]=='$'){
			result.add(node.label);
		}
		else{
			Node child = node.child;
			while(null!=child){
				result.addAll(getNodeAllLeafSonLabel(child));
				child=child.brother;
			}
		}
		return result;
	}

	/**
	 * 查找给定字符串的最长前缀，并返回其alignment效果最好的起始位置和长度，返回(pos,len)对
	 * wordstartpos是输入word在原始序列中开始的位置
	 * @param word
	 * @return
	 */
	public int[] selectPrefixForAlignment(String word, int wordstartpos) {
		int[] back = new int[2];
		back[0]=-1;
		back[1]=0;
		word=word.substring(wordstartpos);

		char[] chars = word.toCharArray();
		int index = 0;// 查找到的节点的匹配的位置
		// 查找从根节点开始，遍历子节点
		Node start = root;

		for (int i = 0; i < chars.length; i++) {
			if (start.chars.length < index + 1) {// 如果当前节点已匹配完，则从子节点开始，同时需重置index==0
				index = 0;
				start = start.child;

				while (null != start) {
					// 比较当前节点指定位置(index)的字符是否与待查找字符一致
					// 由于是遍历子节点，所以如果不匹配换个子节点继续

					if (start.chars.length>index&&start.chars[index] == chars[i]) {
						index++;
						break;
					} else if(null != start.brother){
						start = start.brother;
					}
					else if (null == start.brother&&i>=minMatchLen){ //断在一个结点，每一个儿子都没能继续匹配上

						Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start.father).toArray(new Integer[getNodeAllLeafSonLabel(start.father).size()]);
						int mindis=Integer.MAX_VALUE;
						int pos_j=-1;


						for(int j=0;j<startpos.length;j++){
							int tmp=Math.abs(startpos[j].intValue()-wordstartpos)-i;
							if(tmp<mindis){
								mindis=tmp;
								pos_j=startpos[j].intValue();
							}
						}

						if(mindis<-minMatchLen){
							back[0]=pos_j;
							back[1]=i;
						}
						else{
							back[0]=-1;
							back[1]=0;
						}

						return back;
					}else if(i<minMatchLen){
						back[0]=-1;
						back[1]=0;

						return back;

					}
				}

			} else if (start.chars[index] == chars[i]) {
				// 如果当前查找到的节点的还有可比较字符，则进行比较，如果不同则直接返回false
				index++;
			} else if (i>=minMatchLen){
				//断在一条边的中间，或叶节点，因为叶节点是$,肯定不同，所以在这结束

				Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start).toArray(new Integer[getNodeAllLeafSonLabel(start).size()]);
				int mindis=Integer.MAX_VALUE;
				int pos_j=-1;

				for(int j=0;j<startpos.length;j++){
					int tmp=Math.abs(startpos[j].intValue()-wordstartpos)-i;
					if(tmp<mindis){
						mindis=tmp;
						pos_j=startpos[j].intValue();

					}
				}

				if(mindis<-minMatchLen){
					back[0]=pos_j;
					back[1]=i;
				}
				else{
					back[0]=-1;
					back[1]=0;
				}

				return back;
			} else if(i<minMatchLen){
				back[0]=-1;
				back[1]=0;

				return back;
			}
		}
		//i到头了


		Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start).toArray(new Integer[getNodeAllLeafSonLabel(start).size()]);
		int mindis=Integer.MAX_VALUE;
		int pos_j=-1;
		for(int j=0;j<startpos.length;j++){
			int tmp=Math.abs(startpos[j].intValue()-wordstartpos);
			if(tmp<mindis){
				mindis=tmp;
				pos_j=startpos[j].intValue();
			}
		}

		back[0]=pos_j;
		back[1]=chars.length;
		return back;
	}

	/**
	 * 格式化打印出整个后缀树
	 */
	public void print() {
		Node child = root.child;
		System.out.println("[root] [activePoint:(" + activePoint.point + "," + activePoint.index + ","
				+ activePoint.length + ")], [reminder:" + reminder + "]");
		while (child != null) {
			System.out.print("|——");
			child.print("    ");
			child = child.brother;
		}
	}

	/**
	 * <p>
	 * 后缀树的节点，即边
	 * 每个节点的chars是其父亲到该点的边的字符串，root节点没父亲，所以chars=“”
	 * </p>
	 */
	private class Node {
		public char[] chars;
		public Node child;
		public Node brother;
		public Node father;
		public Node suffixNode;
		//public ArrayList <Integer> position;  //用来记录内部节点所有后代的叶节点代表的后缀的起始位置，如果是叶节点就只有一个整数值，就是它代表的后缀的起始位置
		public int label;//记录叶节点代表的后缀的起始位置，内部结点可能也有值，要通过chars的最后是否是$来判断
		public Node(char[] chars) {
			this.chars = chars;
			//position = new ArrayList();
		}

		@Override
		public String toString() {
			//return "Node [chars=" + String.valueOf(chars) + "]"+"position:"+String.valueOf(position);
			return "Node [chars=" + String.valueOf(chars) + "]";
		}

		public void print(String prefix) {
			System.out.print(String.valueOf(chars));
			if(chars[chars.length-1]=='$'){
				System.out.print(label);
			}
			if (null != this.suffixNode) {
				System.out.println("--" + String.valueOf(this.suffixNode.chars));
			} else {
				System.out.println();
			}
			Node child = this.child;
			while (null != child) {
				System.out.print(prefix + "|——");
				child.print(prefix + prefix);
				child = child.brother;
			}
		}
	}

	/**
	 * <p>
	 * 活动点(active point)，一个三元组：(active_node,active_edge,active_length)
	 * 活动边的父亲是point,儿子是index；活动点应该是point,提取存储的字符串要从儿子index的chars中找(0,length)
	 * </p>
	 */
	private class ActivePoint {
		public Node point;
		public Node index;
		public int length;

		public ActivePoint(Node point, Node index, int length) {
			this.point = point;
			this.index = index;
			this.length = length;
		}

		@Override
		public String toString() {
			return "ActivePoint [point=" + point + ", index=" + index + ", length=" + length + "]";
		}
	}
	/**
	 * 对String格式化,删除非法字符(只保留agctn,其余字符全部替换成n),全部转换成小写,u全部换成t
	 * @param s
	 * @return
	 */
	private static String format(String s){
		s=s.toLowerCase();
		s=s.replace('u', 't');
		StringBuffer sb = new StringBuffer(s);

		for(int i=0;i<sb.length();i++){
			switch(sb.charAt(i)){
				case 'a': break;
				case 'c': break;
				case 'g': break;
				case 't': break;
				case 'n':break;
				default: sb=sb.replace(i, i+1, "n");
			}
		}


		return(sb.toString());
	}

	/**
	 * 输入一个节点，输出从跟到该结点的边上的字符串
	 * @param start
	 * @return
	 */

	public static String getNodeString(Node start){
		String s="";

		while(start.chars.length>0){
			s=String.valueOf(start.chars)+s;
			start=start.father;
		}
		s=String.valueOf(start.chars)+s;
		return s;
	}


	public static void main(String[] args) {

		SuffixTree center = new SuffixTree();
		//String s1 ="ACACCGATGAGTCTGTCACGCGATAGCATGACGCTGCACCCTATGCTCGATAGCATTGCGAC";
		//String s1 ="ACACCGATGAGTCTGTCACGCGATAGCATGAC";
		//String s1 ="ACCACAACACCACAACACCACCACAACACCACCAACCACCT";
		String s1 ="ACACCGATGAGTCTGTCACGCGATAGCTCGACGCTGCACCCTATGCTCGATAGCATTGCGACC";
		//String s1="GGGAGCCATGCATT";
		s1=format(s1)+"$";
		center.build(s1);
		//center.print();

		//System.out.println(String.valueOf(center.root.child));

		//System.out.println(String.valueOf(center.root.child.chars));
	/*
		Node start=center.root.child.brother.child.child.child;
		Integer[] startpos= (Integer[]) getNodeAllLeafSonLabel(start).toArray(new Integer[getNodeAllLeafSonLabel(start).size()]);
		System.out.println(Arrays.toString(startpos));
		System.out.println(getNodeString(start));
	*/
		//System.out.println(center.select("CACAAC"));
		String word = "ACACCGATGAGTCTGTCACGCGATAGCTCGACGCTGCACCCTATGCTCGATAGCATTGCGACC";
		word = word.toLowerCase();
		System.out.println(Arrays.toString(center.selectPrefixForAlignment(word, 0)));

		System.out.println("OK!");
	}
}
