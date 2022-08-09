#include <iostream>
#include <string>
#include <vector>
#include <utility>
#include <cassert>
#include <cstdlib>
#include <tuple>
#include <ctime>

using namespace std;


vector<string> highs;
vector<string> meds;
vector<string> lows;

void getData() {
	string line;
	while ( getline(cin, line) ) {
		if ( line.find("HighTerm:") == 0 ) {
			size_t begin = line.find(" ") + 1;
			size_t end = line.find(" ", begin);
			line = line.substr(begin, end - begin);
			highs.push_back(line);

		} else if ( line.find("MedTerm:") == 0 ) {
			size_t begin = line.find(" ") + 1;
			size_t end = line.find(" ", begin);
			line = line.substr(begin, end - begin);
			meds.push_back(line);

		} else if ( line.find("LowTerm:") == 0 ) {
			size_t begin = line.find(" ") + 1;
			size_t end = line.find(" ", begin);
			line = line.substr(begin, end - begin);
			lows.push_back(line);
		}
	}
}

vector<string> getRandoms(vector<string> &v, size_t N) {
	assert(v.size() >= N);
	vector<string> res;
	for (size_t i = 0; i < N; i ++) {
		int j = rand() % v.size();
		res.push_back(v[j]);
		v[j] = v.back();
		v.pop_back();
	}
	return res;
}

vector<pair<string, vector<string>>> gen(char RF, size_t N, char ONF) {
	vector<pair<string, vector<string>>> res;
	const vector<string> &rlist = 
		RF == 'H' ? highs : 
		RF == 'M' ? meds : lows;
	const vector<string> &onlist =
		ONF == 'H' ? highs :
		ONF == 'M' ? meds : lows;

	vector<string> rlistBuffer = rlist;
	vector<string> onlistBuffer = onlist;
	rlistBuffer = getRandoms(rlistBuffer, 200);

	for (auto rit = rlistBuffer.begin(); rit != rlistBuffer.end(); rit ++) {
		if ( onlistBuffer.size() < N ) onlistBuffer = onlist;
		res.push_back(make_pair(*rit, getRandoms(onlistBuffer, N)));
	}
	
	return res;
}

ostream &output(ostream &out, const vector<pair<string, vector<string>>> &v, 
		const string &name) {
	for (auto it = v.begin(); it != v.end(); it ++) {
		out << name << ": ";
		out << "+" << it->first << " ";
		const vector<string> &onlist = it->second;
		for (size_t i = 0; i < onlist.size(); i ++) {
			if ( name.rfind("Not") + 3 == name.length() ) out << "-";
			out << onlist[i] << " ";
		}
		out << endl;
	}
	return out;
}


// ==========================================================
vector<tuple<string, vector<string>, vector<string>>> gen(char RF, size_t O, char OF, size_t N, char NF) {
	vector<tuple<string, vector<string>, vector<string>>> res;
	const vector<string> &rlist = 
		RF == 'H' ? highs : 
		RF == 'M' ? meds : lows;
	const vector<string> &olist =
		OF == 'H' ? highs :
		OF == 'M' ? meds : lows;
	const vector<string> &nlist =
		NF == 'H' ? highs :
		NF == 'M' ? meds : lows;

	vector<string> rlistBuffer = rlist;
	vector<string> olistBuffer = olist;
	vector<string> nlistBuffer = nlist;
	rlistBuffer = getRandoms(rlistBuffer, 200);

	for (auto rit = rlistBuffer.begin(); rit != rlistBuffer.end(); rit ++) {
		if ( olistBuffer.size() < O ) olistBuffer = olist;
		if ( nlistBuffer.size() < N ) nlistBuffer = nlist;
		res.push_back(make_tuple(*rit, getRandoms(olistBuffer, O), getRandoms(nlistBuffer, N)));
	}
	
	return res;
}

ostream &output(ostream &out, const vector<tuple<string, vector<string>, vector<string>>> &v, 
		const string &name) {
	for (auto it = v.begin(); it != v.end(); it ++) {

		out << name << ": ";
		out << "+" << get<0>(*it) << " ";
		const vector<string> &olist = get<1>(*it);
    const vector<string> &nlist = get<2>(*it);

		for (size_t i = 0; i < olist.size(); i ++) {
			out << olist[i] << " ";
		}
    for (size_t i = 0; i < nlist.size(); i ++) {
      out << "-" << nlist[i] << " ";
    }
		out << endl;
	}
	return out;
}


string dict[256];

ostream &output(ostream &out, char RF, size_t N, char ONF, char ON) {
	string name = 
		dict[int(RF)] + "And" + to_string(N) + dict[int(ONF)] + dict[int(ON)];
	auto res = gen(RF, N, ONF);
	output(out, res, name);
	return out;
}

ostream &output(ostream &out, char RF, size_t O, char OF, size_t N, char NF) {
	string name = dict[int(RF)] + "And"
    + to_string(O) + dict[int(OF)] + "Or"
    + to_string(N) + dict[int(NF)] + "Not";
	auto res = gen(RF, O, OF, N, NF);
	output(out, res, name);
	return out;
}


int main() {
	srand(time(NULL));
	getData();
	dict['H'] = "High";
	dict['M'] = "Med";
	dict['L'] = "Low";
	dict['O'] = "Or";
	dict['N'] = "Not";

  
  /*
  const int SOME = 6;

  output(cout, 'H', SOME, 'H', SOME, 'H');
  output(cout, 'H', SOME, 'H', SOME, 'M');
  output(cout, 'H', SOME, 'H', SOME, 'L');
  output(cout, 'H', SOME, 'M', SOME, 'H');
  output(cout, 'H', SOME, 'M', SOME, 'M');
  output(cout, 'H', SOME, 'M', SOME, 'L');
  output(cout, 'H', SOME, 'L', SOME, 'H');
  output(cout, 'H', SOME, 'L', SOME, 'M');
  output(cout, 'H', SOME, 'L', SOME, 'L');

  output(cout, 'M', SOME, 'H', SOME, 'H');
  output(cout, 'M', SOME, 'H', SOME, 'M');
  output(cout, 'M', SOME, 'H', SOME, 'L');
  output(cout, 'M', SOME, 'M', SOME, 'H');
  output(cout, 'M', SOME, 'M', SOME, 'M');
  output(cout, 'M', SOME, 'M', SOME, 'L');
  output(cout, 'M', SOME, 'L', SOME, 'H');
  output(cout, 'M', SOME, 'L', SOME, 'M');
  output(cout, 'M', SOME, 'L', SOME, 'L');

  output(cout, 'L', SOME, 'H', SOME, 'H');
  output(cout, 'L', SOME, 'H', SOME, 'M');
  output(cout, 'L', SOME, 'H', SOME, 'L');
  output(cout, 'L', SOME, 'M', SOME, 'H');
  output(cout, 'L', SOME, 'M', SOME, 'M');
  output(cout, 'L', SOME, 'M', SOME, 'L');
  output(cout, 'L', SOME, 'L', SOME, 'H');
  output(cout, 'L', SOME, 'L', SOME, 'M');
  output(cout, 'L', SOME, 'L', SOME, 'L');
  */
  

  
  for (int SOME = 3; SOME <= 9; SOME ++) {
	output(cout, 'H', SOME, 'H', 'O');
	output(cout, 'H', SOME, 'M', 'O');
	output(cout, 'H', SOME, 'L', 'O');
	output(cout, 'M', SOME, 'H', 'O');
	output(cout, 'M', SOME, 'M', 'O');
	output(cout, 'M', SOME, 'L', 'O');
	output(cout, 'L', SOME, 'H', 'O');
	output(cout, 'L', SOME, 'M', 'O');
	output(cout, 'L', SOME, 'L', 'O');

//	output(cout, 'H', SOME, 'H', 'O');

	output(cout, 'H', SOME, 'H', 'N');
	output(cout, 'H', SOME, 'M', 'N');
	output(cout, 'H', SOME, 'L', 'N');
	output(cout, 'M', SOME, 'H', 'N');
	output(cout, 'M', SOME, 'M', 'N');
	output(cout, 'M', SOME, 'L', 'N');
	output(cout, 'L', SOME, 'H', 'N');
	output(cout, 'L', SOME, 'M', 'N');
	output(cout, 'L', SOME, 'L', 'N');

  }

	return 0;
}


