// Copyright 2016, National University of Defense Technology
// Authors: Xuhao Chen <cxh@illinois.edu> and Pingfan Li <lipingfan@163.com>

#include <vector>
#include <set>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string.h>
#include <algorithm>
#include "common.h"

struct Edge {
	IndexType dst;
	WeightT wt;
};

bool compare_id(Edge a, Edge b) { return (a.dst < b.dst); }

void fill_data(int m, int &nnz, IndexType *&row_offsets, IndexType *&column_indices, WeightT *&weight, vector<vector<Edge> > vertices, bool symmetrize, bool sorted, bool remove_selfloops, bool remove_redundents) {
/*
	//sort the neighbor list
	if(sorted) {
		printf("Sorting the neighbor lists...");
		for(int i = 0; i < m; i++) {
			std::sort(vertices[i].begin(), vertices[i].end(), compare_id);
		}
		printf(" Done\n");
	}

	//remove self loops
	int num_selfloops = 0;
	if(remove_selfloops) {
		printf("Removing self loops...");
		for(unsigned i = 0; i < (unsigned)m; i++) {
			for(unsigned j = 0; j < vertices[i].size(); j ++) {
				if(i == vertices[i][j].dst) {
					vertices[i].erase(vertices[i].begin()+j);
					num_selfloops ++;
					j --;
				}
			}
		}
		printf(" %d selfloops are removed\n", num_selfloops);
	}

	// remove redundent
	int num_redundents = 0;
	if(remove_redundents) {
		printf("Removing redundent edges...");
		for (unsigned i = 0; i < (unsigned)m; i++) {
			for (unsigned j = 1; j < vertices[i].size(); j ++) {
				if (vertices[i][j].dst == vertices[i][j-1].dst) {
					vertices[i].erase(vertices[i].begin()+j);
					num_redundents ++;
					j --;
				}
			}
		}
		printf(" %d redundent edges are removed\n", num_redundents);
	}
*/
	row_offsets = (IndexType *)malloc((m + 1) * sizeof(IndexType));
	int count = 0;
	for (int i = 0; i < m; i++) {
		row_offsets[i] = count;
		count += vertices[i].size();
	}
	row_offsets[m] = count;
	nnz = count;
	printf("num_vertices %d num_edges %d\n", m, nnz);
	column_indices = (IndexType *)malloc(count * sizeof(IndexType));
	weight = (WeightT *)malloc(count * sizeof(WeightT));
	vector<Edge>::iterator neighbor_list;
	for (int i = 0, index = 0; i < m; i++) {
		neighbor_list = vertices[i].begin();
		while (neighbor_list != vertices[i].end()) {
			column_indices[index] = (*neighbor_list).dst;
			weight[index] = (*neighbor_list).wt;
			index ++;
			neighbor_list ++;
		}
	}
}

// transfer mtx graph to CSR format
void mtx2csr(const char *mtx, int &m, int &n, int &nnz, IndexType *&row_offsets, IndexType *&column_indices, WeightT *&weight, bool symmetrize, bool transpose, bool sorted, bool remove_selfloops, bool remove_redundents) {
	printf("Reading (.mtx) input file %s\n", mtx);
	std::ifstream cfile;
	cfile.open(mtx);
	std::string str;
	getline(cfile, str);
	char c;
	sscanf(str.c_str(), "%c", &c);
	while (c == '%') {
		getline(cfile, str);
		sscanf(str.c_str(), "%c", &c);
	}
	sscanf(str.c_str(), "%d %d %d", &m, &n, &nnz);
	if (m != n) {
		printf("Warning, m(%d) != n(%d)\n", m, n);
	}
	vector<vector<Edge> > vertices;
	vector<Edge> neighbors;
	for (int i = 0; i < m; i ++)
		vertices.push_back(neighbors);
	IndexType dst, src;
	WeightT wt = 1.0f;
	for (int i = 0; i < nnz; i ++) {
		getline(cfile, str);
		int num = sscanf(str.c_str(), "%d %d %f", &src, &dst, &wt);
		if (num == 2) wt = 1;
		if (wt < 0) wt = -wt; // non-negtive weight
		src--;
		dst--;
		Edge e1, e2;
		if(symmetrize && src != dst) {
			e2.dst = src; e2.wt = wt;
			vertices[dst].push_back(e2);
			transpose = false;
		}
		if(!transpose) {
			e1.dst = dst; e1.wt = wt;
			vertices[src].push_back(e1);
		} else {
			e1.dst = src; e1.wt = wt;
			vertices[dst].push_back(e1);
		}
	}
	cfile.close();
	fill_data(m, nnz, row_offsets, column_indices, weight, vertices, symmetrize, sorted, remove_selfloops, remove_redundents);
}

void read_graph(int argc, char *argv[], int &m, int &n, int &nnz, IndexType *&row_offsets, IndexType *&column_indices, int *&degree, WeightT *&weight, bool is_symmetrize=false, bool is_transpose=false, bool sorted=true, bool remove_selfloops=true, bool remove_redundents=true) {
	//char *filename = argv[1];
	const char *filename = string("test_spmv.mtx").c_str();
	mtx2csr(filename, m, n, nnz, row_offsets, column_indices, weight, is_symmetrize, is_transpose, sorted, remove_selfloops, remove_redundents);
}

