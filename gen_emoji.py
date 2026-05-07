import urllib.request
import os.path

EMOJIS_PATH = 'res/raw/emojis.txt'
EMOJIS_SKINTONE_MODIFIABLE_PATH = 'res/raw/emojis_skintone_modifiable.txt'
EMOJI_TEST_PATH = 'emoji-test.txt'
EMOJI_TEST_URL = 'https://unicode.org/Public/emoji/latest/emoji-test.txt'

EMOJI_SKINTONE_MODIFIERS = [ 0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF ]

def rawEmojiFromCodes(codes):
    return ''.join([chr(int(c, 16)) for c in codes])

def rawEmojiFromValues(values):
    return ''.join([chr(v) for v in values])

def getEmojiTestContents():
    if os.path.exists(EMOJI_TEST_PATH):
        print(f'Using existing {EMOJI_TEST_PATH}')
    else:
        print(f'Downloading {EMOJI_TEST_URL}')
        urllib.request.urlretrieve(EMOJI_TEST_URL, EMOJI_TEST_PATH)
    return open(EMOJI_TEST_PATH, mode='r', encoding='UTF-8').read()
        

emoji_list = []
emoji_skintone_modifiable_list = []
group_indices = []
for line in getEmojiTestContents().splitlines():
    if line.startswith('# group:'):
        if len(group_indices) == 0 or len(emoji_list) > group_indices[-1]:
            group_indices.append(len(emoji_list))
    elif not line.startswith('#') and 'fully-qualified' in line:
        codes = line.split(';')[0].split()
        values = [int(c, 16) for c in codes]
        emoji = rawEmojiFromValues(values)

        # if any of the values starting at index 1 are skintone modifiers
        if any(v in EMOJI_SKINTONE_MODIFIERS for v in values[1:]):
            # emoji_skintone_modifiable_list is a list of items, with every
            # item being another list, starting with the index of the emoji and followed by all the
            # skintones that exist for this emoji

            # if the list is empty, or the last entry doesnt begin with the index we are currently at
            if len(emoji_skintone_modifiable_list) == 0 or emoji_skintone_modifiable_list[-1][0] != len(emoji_list)-1:
                # create a new list with the index
                emoji_skintone_modifiable_list.append([len(emoji_list)-1, emoji_list[-1]])
            # add the current skintone to the newest list
            emoji_skintone_modifiable_list[-1].append(emoji)        
        else:
            emoji_list.append(emoji)

with open(EMOJIS_PATH, 'w', encoding='UTF-8') as emojis:
    for e in emoji_list:
        emojis.write(f'{e}\n')
    emojis.write('\n')

    emojis.write(' '.join([str(g) for g in group_indices]))
    emojis.write('\n')

with open(EMOJIS_SKINTONE_MODIFIABLE_PATH, 'w', encoding='UTF-8') as emojis:
    for es in emoji_skintone_modifiable_list:
        for e in es:
            emojis.write(f'{e}\n')
        emojis.write('\n')
    
print(f'Parsed {len(emoji_list)} emojis in {len(group_indices)}')
